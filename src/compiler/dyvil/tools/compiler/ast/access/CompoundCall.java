package dyvil.tools.compiler.ast.access;

import dyvil.reflect.Modifiers;
import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.ASTNode;
import dyvil.tools.compiler.ast.constant.INumericValue;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.expression.IValued;
import dyvil.tools.compiler.ast.field.IField;
import dyvil.tools.compiler.ast.field.IVariable;
import dyvil.tools.compiler.ast.generic.GenericData;
import dyvil.tools.compiler.ast.member.INamed;
import dyvil.tools.compiler.ast.member.Name;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.parameter.EmptyArguments;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.PrimitiveType;
import dyvil.tools.compiler.ast.type.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.lexer.marker.Marker;
import dyvil.tools.compiler.lexer.marker.MarkerList;
import dyvil.tools.compiler.lexer.position.ICodePosition;

public final class CompoundCall extends ASTNode implements ICall, INamed, IValued
{
	public Name			name;
	
	public IValue		instance;
	public IArguments	arguments	= EmptyArguments.INSTANCE;
	
	public IMethod		method;
	public IMethod		updateMethod;
	private GenericData	genericData;
	private IType		type;
	
	public CompoundCall(ICodePosition position)
	{
		this.position = position;
	}
	
	public CompoundCall(ICodePosition position, IValue instance, Name name)
	{
		this.position = position;
		this.instance = instance;
		this.name = name;
	}
	
	private GenericData getGenericData()
	{
		if (this.method == null || this.genericData != null && this.genericData.computedGenerics >= 0)
		{
			return this.genericData;
		}
		return this.genericData = this.method.getGenericData(this.genericData, this.instance, this.arguments);
	}
	
	@Override
	public int valueTag()
	{
		return METHOD_CALL;
	}
	
	@Override
	public IType getType()
	{
		if (this.method == null)
		{
			return Types.UNKNOWN;
		}
		if (this.type == null)
		{
			this.type = this.method.getType().getConcreteType(this.getGenericData());
			
			if (this.method.isIntrinsic() && (this.instance == null || this.instance.getType().isPrimitive()))
			{
				this.type = PrimitiveType.getPrimitiveType(this.type);
			}
		}
		return this.type;
	}
	
	@Override
	public IValue withType(IType type)
	{
		return type == Types.VOID ? this : ICall.super.withType(type);
	}
	
	@Override
	public boolean isType(IType type)
	{
		if (type == Types.VOID)
		{
			return true;
		}
		if (this.method == null)
		{
			return false;
		}
		return type.isSuperTypeOf(this.getType());
	}
	
	@Override
	public int getTypeMatch(IType type)
	{
		if (this.method == null)
		{
			return 0;
		}
		
		IType type1 = this.method.getType();
		if (type.equals(type1))
		{
			return 3;
		}
		if (type.isSuperTypeOf(type1))
		{
			return 2;
		}
		return 0;
	}
	
	@Override
	public void setName(Name name)
	{
		this.name = name;
	}
	
	@Override
	public Name getName()
	{
		return this.name;
	}
	
	@Override
	public void setValue(IValue value)
	{
		this.arguments = this.arguments.addLastValue(Name.update, value);
	}
	
	@Override
	public IValue getValue()
	{
		return null;
	}
	
	@Override
	public void setArguments(IArguments arguments)
	{
		this.arguments = arguments;
	}
	
	@Override
	public IArguments getArguments()
	{
		return this.arguments;
	}
	
	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		if (this.instance != null)
		{
			this.instance.resolveTypes(markers, context);
		}
		
		this.arguments.resolveTypes(markers, context);
	}
	
	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		if (this.instance != null)
		{
			this.instance.resolve(markers, context);
		}
		
		this.arguments.resolve(markers, context);
		
		IMethod method = ICall.resolveMethod(context, this.instance, this.name, this.arguments);
		if (method != null)
		{
			this.method = method;
			return this;
		}
		
		Marker marker = markers.create(this.position, "resolve.method", this.name.unqualified);
		marker.addInfo("Qualified Name: " + this.name.unqualified);
		if (this.instance != null)
		{
			marker.addInfo("Callee Type: " + this.instance.getType());
		}
		if (!this.arguments.isEmpty())
		{
			StringBuilder builder = new StringBuilder("Argument Types: ");
			this.arguments.typesToString(builder);
			marker.addInfo(builder.toString());
		}
		return this;
	}
	
	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		if (this.instance != null)
		{
			this.instance.checkTypes(markers, context);
			
			int valueTag = this.instance.valueTag();
			if (valueTag == APPLY_METHOD_CALL)
			{
				ApplyMethodCall call = (ApplyMethodCall) this.instance;
				IValue instance1 = call.instance;
				IArguments arguments1 = call.arguments.addLastValue(call);
				
				IType type = instance1.getType();
				IMethod match = IContext.resolveMethod(type, instance1, Name.update, arguments1);
				if (match != null)
				{
					this.updateMethod = match;
				}
				else
				{
					Marker marker = markers.create(this.position, "method.compound.update");
					marker.addInfo("Callee Type: " + type);
				}
			}
			else if (valueTag == FIELD_ACCESS)
			{
				FieldAccess fa = (FieldAccess) this.instance;
				if (fa.field != null)
				{
					fa.field.checkAssign(markers, this.instance.getPosition(), this.instance, this);
				}
			}
		}
		
		if (this.method != null)
		{
			this.method.checkArguments(markers, this.instance, this.arguments, this.getGenericData());
		}
		this.arguments.checkTypes(markers, context);
	}
	
	@Override
	public void check(MarkerList markers, IContext context)
	{
		if (this.instance != null)
		{
			this.instance.check(markers, context);
		}
		
		if (this.method != null)
		{
			IType type1 = this.instance.getType();
			IType type2 = this.getType();
			if (!type1.isSuperTypeOf(type2))
			{
				Marker marker = markers.create(this.position, "method.compound.type", this.name, this.instance.toString());
				marker.addInfo("Callee Type: " + type1);
				marker.addInfo("Method Type: " + type2);
			}
			
			if (this.method.hasModifier(Modifiers.DEPRECATED))
			{
				markers.add(this.position, "method.access.deprecated", this.name.unqualified);
			}
			
			switch (context.getVisibility(this.method))
			{
			case IContext.STATIC:
				markers.add(this.position, "method.access.instance", this.name.unqualified);
				break;
			case IContext.SEALED:
				markers.add(this.position, "method.access.sealed", this.name.unqualified);
				break;
			case IContext.INVISIBLE:
				markers.add(this.position, "method.access.invisible", this.name.unqualified);
				break;
			}
		}
		
		this.arguments.check(markers, context);
	}
	
	@Override
	public IValue foldConstants()
	{
		if (this.instance != null)
		{
			this.instance = this.instance.foldConstants();
		}
		this.arguments.foldConstants();
		return this;
	}
	
	@Override
	public void writeExpression(MethodWriter writer) throws BytecodeException
	{
		int i = this.instance.valueTag();
		if (i == FIELD_ACCESS)
		{
			FieldAccess access = (FieldAccess) this.instance;
			IField f = access.field;
			
			if (this.writeIINC(writer, f))
			{
				f.writeGet(writer, null);
				return;
			}
			
			IValue instance = access.instance;
			if (instance != null)
			{
				instance.writeExpression(writer);
				writer.writeInsn(Opcodes.AUTO_DUP);
			}
			
			f.writeGet(writer, null);
			this.method.writeCall(writer, null, this.arguments, null);
			writer.writeInsn(Opcodes.AUTO_DUP);
			f.writeSet(writer, null, null);
		}
		else if (i == APPLY_METHOD_CALL)
		{
			ApplyMethodCall call = (ApplyMethodCall) this.instance;
			
			call.instance.writeExpression(writer);
			
			for (IValue v : call.arguments)
			{
				v.writeExpression(writer);
			}
			
			writer.writeInsn(Opcodes.DUP2);
			
			call.method.writeCall(writer, null, EmptyArguments.INSTANCE, null);
			this.method.writeCall(writer, null, this.arguments, null);
			writer.writeInsn(Opcodes.DUP_X2);
			this.updateMethod.writeCall(writer, null, EmptyArguments.INSTANCE, null);
		}
	}
	
	@Override
	public void writeStatement(MethodWriter writer) throws BytecodeException
	{
		int i = this.instance.valueTag();
		if (i == FIELD_ACCESS)
		{
			FieldAccess access = (FieldAccess) this.instance;
			IField f = access.field;
			
			if (this.writeIINC(writer, f))
			{
				return;
			}
			
			IValue instance = access.instance;
			if (instance != null)
			{
				instance.writeExpression(writer);
				writer.writeInsn(Opcodes.AUTO_DUP);
			}
			
			f.writeGet(writer, null);
			this.method.writeCall(writer, null, this.arguments, null);
			f.writeSet(writer, null, null);
		}
		else if (i == APPLY_METHOD_CALL)
		{
			ApplyMethodCall call = (ApplyMethodCall) this.instance;
			
			call.instance.writeExpression(writer);
			
			for (IValue v : call.arguments)
			{
				v.writeExpression(writer);
			}
			
			writer.writeInsn(Opcodes.DUP2);
			
			call.method.writeCall(writer, null, EmptyArguments.INSTANCE, null);
			this.method.writeCall(writer, null, this.arguments, null);
			this.updateMethod.writeCall(writer, null, EmptyArguments.INSTANCE, null);
		}
	}
	
	private boolean writeIINC(MethodWriter writer, IField f) throws BytecodeException
	{
		if (this.arguments.size() == 1 && f.getType() == Types.INT && f.isVariable())
		{
			boolean minus = false;
			if (this.name == Name.plus || (minus = this.name == Name.minus))
			{
				IValue value1 = this.arguments.getFirstValue();
				if (IValue.isNumeric(value1.valueTag()))
				{
					int count = ((INumericValue) value1).intValue();
					writer.writeIINC(((IVariable) f).getIndex(), minus ? -count : count);
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		if (this.instance != null)
		{
			this.instance.toString(prefix, buffer);
			buffer.append(' ');
		}
		
		if (Formatting.Method.convertQualifiedNames)
		{
			buffer.append(this.name.qualified).append("$eq");
		}
		else
		{
			buffer.append(this.name.unqualified).append('=');
		}
		
		this.arguments.toString(prefix, buffer);
	}
}
