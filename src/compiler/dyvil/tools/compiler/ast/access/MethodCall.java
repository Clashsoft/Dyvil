package dyvil.tools.compiler.ast.access;

import org.objectweb.asm.Label;

import dyvil.reflect.Modifiers;
import dyvil.tools.compiler.ast.ASTNode;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.expression.IValued;
import dyvil.tools.compiler.ast.expression.MatchExpression;
import dyvil.tools.compiler.ast.field.IField;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.member.INamed;
import dyvil.tools.compiler.ast.member.Name;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.operator.Operators;
import dyvil.tools.compiler.ast.parameter.EmptyArguments;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.type.*;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.lexer.marker.Marker;
import dyvil.tools.compiler.lexer.marker.MarkerList;
import dyvil.tools.compiler.lexer.position.ICodePosition;
import dyvil.tools.compiler.transform.ConstantFolder;
import dyvil.tools.compiler.util.Util;

public final class MethodCall extends ASTNode implements ICall, IValued, INamed, ITypeList, ITypeContext
{
	public IValue		instance;
	public boolean		dotless;
	public Name			name;
	public IType[]		generics;
	public int			genericCount;
	
	public IArguments	arguments	= EmptyArguments.INSTANCE;
	
	public IMethod		method;
	private IType		type;
	
	public MethodCall(ICodePosition position)
	{
		this.position = position;
	}
	
	public MethodCall(ICodePosition position, IValue instance, Name name)
	{
		this.position = position;
		this.instance = instance;
		
		this.name = name;
	}
	
	@Override
	public int getValueType()
	{
		return METHOD_CALL;
	}
	
	@Override
	public boolean isPrimitive()
	{
		return this.method != null && (this.method.isIntrinsic() || this.getType().isPrimitive());
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
			if (this.method.hasTypeVariables())
			{
				this.type = this.method.getType(this);
			}
			this.type = this.method.getType();
			
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
		this.instance = value;
	}
	
	@Override
	public IValue getValue()
	{
		return this.instance;
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
	public int typeCount()
	{
		return this.genericCount;
	}
	
	@Override
	public void setType(int index, IType type)
	{
		this.generics[index] = type;
	}
	
	@Override
	public void addType(IType type)
	{
		if (this.generics == null)
		{
			this.generics = new IType[3];
			this.generics[0] = type;
			this.genericCount = 1;
			return;
		}
		
		int index = this.genericCount++;
		if (this.genericCount > this.generics.length)
		{
			IType[] temp = new IType[this.genericCount];
			System.arraycopy(this.generics, 0, temp, 0, index);
			this.generics = temp;
		}
		this.generics[index] = type;
	}
	
	@Override
	public IType getType(int index)
	{
		return this.generics[index];
	}
	
	@Override
	public IType resolveType(Name name)
	{
		return this.method.resolveType(name, this.instance, this.arguments, this);
	}
	
	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		for (int i = 0; i < this.genericCount; i++)
		{
			this.generics[i] = this.generics[i].resolve(markers, context);
		}
		
		if (this.instance != null)
		{
			this.instance.resolveTypes(markers, context);
		}
		this.arguments.resolveTypes(markers, context);
	}
	
	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		int args = this.arguments.size();
		
		if (this.instance != null)
		{
			this.instance = this.instance.resolve(markers, context);
		}
		
		if (args == 1 && this.name == Name.match)
		{
			MatchExpression me = Operators.getMatchExpression(this.instance, this.arguments.getFirstValue());
			if (me != null)
			{
				me.position = this.position;
				return me.resolve(markers, context);
			}
		}
		
		this.arguments.resolve(markers, context);
		
		if (args == 1)
		{
			IValue argument = this.arguments.getFirstValue();
			IValue op;
			if (this.instance != null)
			{
				op = Operators.get(this.instance, this.name, argument);
			}
			else
			{
				op = Operators.get(this.name, argument);
			}
			if (op != null)
			{
				op.setPosition(this.position);
				return op;
			}
		}
		
		IMethod method = ICall.resolveMethod(markers, context, this.instance, this.name, this.arguments);
		if (method != null)
		{
			this.method = method;
			return this;
		}
		
		if (args == 1 && this.instance != null)
		{
			String qualified = this.name.qualified;
			if (qualified.endsWith("$eq"))
			{
				String unqualified = this.name.unqualified;
				Name name = Name.get(qualified.substring(0, qualified.length() - 3), unqualified.substring(0, unqualified.length() - 1));
				IMethod method1 = IContext.resolveMethod(markers, this.instance.getType(), null, name, this.arguments);
				if (method1 != null)
				{
					CompoundCall call = new CompoundCall(this.position);
					call.method = method1;
					call.instance = this.instance;
					call.arguments = this.arguments;
					call.name = name;
					return call;
				}
			}
		}
		
		if (args == 0)
		{
			IField field = ICall.resolveField(context, this.instance, this.name);
			if (field != null)
			{
				FieldAccess access = new FieldAccess(this.position);
				access.field = field;
				access.instance = this.instance;
				access.name = this.name;
				access.dotless = this.dotless;
				return access;
			}
		}
		// Resolve Apply Method
		else if (this.instance == null)
		{
			IValue apply = this.resolveApply(markers, context);
			if (apply != null)
			{
				return apply;
			}
		}
		
		Marker marker;
		if (this.arguments.isEmpty())
		{
			marker = markers.create(this.position, "resolve.method_field", this.name.unqualified);
		}
		else
		{
			marker = markers.create(this.position, "resolve.method", this.name.unqualified);
		}
		
		marker.addInfo("Qualified Name: " + this.name.qualified);
		if (this.instance != null)
		{
			marker.addInfo("Instance Type: " + this.instance.getType());
		}
		StringBuilder builder = new StringBuilder("Argument Types: ");
		Util.typesToString("", this.arguments, ", ", builder);
		marker.addInfo(builder.toString());
		return this;
	}
	
	private IValue resolveApply(MarkerList markers, IContext context)
	{
		IValue instance;
		IMethod method;
		IType type = null;
		
		IField field = context.resolveField(this.name);
		if (field == null)
		{
			// Find a type
			type = new Type(this.position, this.name).resolve(null, context);
			if (!type.isResolved())
			{
				// No type found -> Not an apply method call
				return null;
			}
			// Find the apply method of the type
			IMethod match = IContext.resolveMethod(markers, type, null, Name.apply, this.arguments);
			if (match == null)
			{
				// No apply method found -> Not an apply method call
				return null;
			}
			method = match;
			instance = new ClassAccess(this.position, type);
		}
		else
		{
			FieldAccess access = new FieldAccess(this.position);
			access.field = field;
			access.name = this.name;
			access.dotless = this.dotless;
			
			// Find the apply method of the field type
			IMethod match = IContext.resolveMethod(markers, field.getType(), access, Name.apply, this.arguments);
			if (match == null)
			{
				// No apply method found -> Not an apply method call
				return null;
			}
			method = match;
			instance = access;
		}
		
		ApplyMethodCall call = new ApplyMethodCall(this.position);
		call.method = method;
		call.instance = instance;
		call.arguments = this.arguments;
		
		return call;
	}
	
	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		if (this.instance != null)
		{
			this.instance.checkTypes(markers, context);
		}
		
		if (this.method != null)
		{
			this.instance = this.method.checkArguments(markers, this.instance, this.arguments, this);
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
			if (this.method.hasModifier(Modifiers.DEPRECATED))
			{
				markers.add(this.position, "access.method.deprecated", this.name);
			}
			
			byte access = context.getAccessibility(this.method);
			if (access == IContext.STATIC)
			{
				markers.add(this.position, "access.method.instance", this.name);
			}
			else if (access == IContext.SEALED)
			{
				markers.add(this.position, "access.method.sealed", this.name);
			}
			else if ((access & IContext.READ_ACCESS) == 0)
			{
				markers.add(this.position, "access.method.invisible", this.name);
			}
		}
		
		this.arguments.check(markers, context);
	}
	
	@Override
	public IValue foldConstants()
	{
		this.arguments.foldConstants();
		if (this.arguments.size() == 1)
		{
			IValue argument = this.arguments.getFirstValue();
			if (argument.isConstant())
			{
				if (this.instance != null)
				{
					if (this.instance.isConstant())
					{
						IValue v1 = ConstantFolder.apply(this.instance, this.name, argument);
						return v1 == null ? this : v1;
					}
					
					this.instance = this.instance.foldConstants();
					return this;
				}
				
				IValue v1 = ConstantFolder.apply(this.name, argument);
				if (v1 != null)
				{
					return v1;
				}
			}
			
			if (this.instance != null)
			{
				this.instance = this.instance.foldConstants();
			}
			return this;
		}
		
		if (this.instance != null)
		{
			this.instance = this.instance.foldConstants();
		}
		return this;
	}
	
	@Override
	public void writeExpression(MethodWriter writer)
	{
		this.method.writeCall(writer, this.instance, this.arguments, this.type);
	}
	
	@Override
	public void writeStatement(MethodWriter writer)
	{
		this.method.writeCall(writer, this.instance, this.arguments, Types.VOID);
	}
	
	@Override
	public void writeJump(MethodWriter writer, Label dest)
	{
		this.method.writeJump(writer, dest, this.instance, this.arguments);
	}
	
	@Override
	public void writeInvJump(MethodWriter writer, Label dest)
	{
		this.method.writeInvJump(writer, dest, this.instance, this.arguments);
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		if (this.instance != null)
		{
			this.instance.toString(prefix, buffer);
			if (this.dotless && !Formatting.Method.useJavaFormat)
			{
				buffer.append(Formatting.Method.dotlessSeperator);
			}
			else
			{
				buffer.append('.');
			}
		}
		
		buffer.append(this.name);
		
		if (this.generics != null)
		{
			buffer.append('[');
			Util.astToString(prefix, this.generics, this.genericCount, Formatting.Type.genericSeperator, buffer);
			buffer.append(']');
		}
		
		this.arguments.toString(prefix, buffer);
	}
}
