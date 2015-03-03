package dyvil.tools.compiler.ast.access;

import java.util.List;

import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.Opcodes;
import dyvil.reflect.Modifiers;
import dyvil.tools.compiler.ast.ASTNode;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Type;
import dyvil.tools.compiler.ast.value.IValue;
import dyvil.tools.compiler.ast.value.IValued;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.lexer.marker.Marker;
import dyvil.tools.compiler.lexer.marker.Markers;
import dyvil.tools.compiler.lexer.position.ICodePosition;

public class ApplyMethodCall extends ASTNode implements IValue, IValued, ITypeContext
{
	public IValue		instance;
	public IArguments	arguments;
	
	public IMethod		method;
	
	public ApplyMethodCall(ICodePosition position)
	{
		this.position = position;
	}
	
	public ApplyMethodCall(ICodePosition position, IValue instance)
	{
		this.position = position;
		this.instance = instance;
	}
	
	@Override
	public int getValueType()
	{
		return APPLY_METHOD_CALL;
	}
	
	@Override
	public boolean isPrimitive()
	{
		return this.method.isIntrinsic() || this.getType().isPrimitive();
	}
	
	@Override
	public IType getType()
	{
		return this.method == null ? Type.NONE : this.method.getType();
	}
	
	@Override
	public boolean isType(IType type)
	{
		if (type == Type.NONE || type == Type.VOID)
		{
			return true;
		}
		return this.method == null ? false : Type.isSuperType(type, this.method.getType());
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
		else if (type.isSuperTypeOf(type1))
		{
			return 2;
		}
		return 0;
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
	
	public void setArguments(IArguments arguments)
	{
		this.arguments = arguments;
	}
	
	public IArguments getArguments()
	{
		return this.arguments;
	}
	
	@Override
	public IType resolveType(String name)
	{
		return this.method.resolveType(name, this.instance, this.arguments, null);
	}
	
	@Override
	public void resolveTypes(List<Marker> markers, IContext context)
	{
		if (this.instance != null)
		{
			this.instance.resolveTypes(markers, context);
		}
		
		for (IValue v : this.arguments)
		{
			v.resolveTypes(markers, context);
		}
	}
	
	@Override
	public IValue resolve(List<Marker> markers, IContext context)
	{
		if (this.instance != null)
		{
			this.instance = this.instance.resolve(markers, context);
		}
		
		this.arguments.resolve(markers, context);
		
		IMethod method = IAccess.resolveMethod(context, this.instance, "apply", this.arguments);
		if (method != null)
		{
			this.method = method;
			return this;
		}
		
		Marker marker = Markers.create(this.position, "resolve.method", "apply");
		
		if (this.instance != null)
		{
			IType vtype = this.instance.getType();
			marker.addInfo("Instance Type: " + (vtype == null ? "unknown" : vtype));
		}
		StringBuilder builder = new StringBuilder("Argument Types: ");
		// FIXME Util.typesToString("", this.arguments, ", ", builder);
		marker.addInfo(builder.toString());
		markers.add(marker);
		return this;
	}
	
	@Override
	public void check(List<Marker> markers, IContext context)
	{
		if (this.instance != null)
		{
			this.instance.check(markers, context);
		}
		
		if (this.arguments != null)
		{
			this.arguments.check(markers, context);
		}
		
		if (this.method != null)
		{
			this.method.checkArguments(markers, this.instance, this.arguments, this);
			
			if (this.method.hasModifier(Modifiers.DEPRECATED))
			{
				markers.add(Markers.create(this.position, "access.method.deprecated", "apply"));
			}
			
			byte access = context.getAccessibility(this.method);
			if (access == IContext.STATIC)
			{
				markers.add(Markers.create(this.position, "access.method.instance", "apply"));
			}
			else if (access == IContext.SEALED)
			{
				markers.add(Markers.create(this.position, "access.method.sealed", "apply"));
			}
			else if ((access & IContext.READ_ACCESS) == 0)
			{
				markers.add(Markers.create(this.position, "access.method.invisible", "apply"));
			}
		}
	}
	
	@Override
	public IValue foldConstants()
	{
		if (this.instance != null)
		{
			this.instance = this.instance.foldConstants();
		}
		
		return this;
	}
	
	@Override
	public void writeExpression(MethodWriter writer)
	{
		this.method.writeCall(writer, this.instance, this.arguments);
	}
	
	@Override
	public void writeStatement(MethodWriter writer)
	{
		this.method.writeCall(writer, this.instance, this.arguments);
		
		if (this.method.getType() != Type.VOID)
		{
			writer.visitInsn(Opcodes.POP);
		}
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
		}
		
		this.arguments.toString(prefix, buffer);
	}
}
