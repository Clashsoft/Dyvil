package dyvil.tools.compiler.ast.expression;

import java.util.List;

import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.Opcodes;
import dyvil.tools.compiler.ast.annotation.Annotation;
import dyvil.tools.compiler.ast.api.*;
import dyvil.tools.compiler.ast.field.FieldMatch;
import dyvil.tools.compiler.ast.method.MethodMatch;
import dyvil.tools.compiler.ast.type.Type;
import dyvil.tools.compiler.ast.value.IntValue;
import dyvil.tools.compiler.ast.value.SuperValue;
import dyvil.tools.compiler.bytecode.MethodWriter;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.lexer.marker.Marker;
import dyvil.tools.compiler.lexer.marker.SemanticError;
import dyvil.tools.compiler.lexer.position.ICodePosition;
import dyvil.tools.compiler.util.*;

public class MethodCall extends Call implements INamed, IValued
{
	public IValue		instance;
	protected String	name;
	protected String	qualifiedName;
	
	public boolean		dotless;
	
	public MethodCall(ICodePosition position)
	{
		super(position);
	}
	
	public MethodCall(ICodePosition position, IValue instance, String name)
	{
		super(position);
		this.instance = instance;
		this.name = name;
		this.qualifiedName = Symbols.expand(name);
	}
	
	@Override
	public IType getType()
	{
		if (this.method == null)
		{
			return null;
		}
		return this.method.getType();
	}
	
	@Override
	public void setName(String name)
	{
		this.name = name;
	}
	
	@Override
	public String getName()
	{
		return this.name;
	}
	
	@Override
	public void setQualifiedName(String name)
	{
		this.qualifiedName = name;
	}
	
	@Override
	public String getQualifiedName()
	{
		return this.qualifiedName;
	}
	
	@Override
	public boolean isName(String name)
	{
		return this.qualifiedName.equals(name);
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
	public void setArray(boolean array)
	{
	}
	
	@Override
	public boolean isArray()
	{
		return false;
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
		int len = this.arguments.size();
		for (int i = 0; i < len; i++)
		{
			IValue v1 = this.arguments.get(i);
			IValue v2 = v1.resolve(markers, context);
			if (v1 != v2)
			{
				this.arguments.set(i, v2);
			}
		}
		
		return AccessResolver.resolve(markers, context, this);
	}
	
	@Override
	public void check(List<Marker> markers, IContext context)
	{
		if (this.instance != null)
		{
			this.instance.check(markers, context);
		}
		
		for (IValue v : this.arguments)
		{
			v.check(markers, context);
		}
		
		if (this.method != null)
		{
			this.method.checkArguments(markers, this.instance, this.arguments);
			
			byte access = context.getAccessibility(this.method);
			if (access == IContext.STATIC)
			{
				markers.add(new SemanticError(this.position, "The instance method '" + this.name + "' cannot be invoked from a static context"));
			}
			else if (access == IContext.SEALED)
			{
				markers.add(new SemanticError(this.position, "The sealed method '" + this.name + "' cannot be invoked because it is private to it's library"));
			}
			else if ((access & IContext.READ_ACCESS) == 0)
			{
				markers.add(new SemanticError(this.position, "The method '" + this.name + "' cannot be invoked since it is not visible"));
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
		
		int len = this.arguments.size();
		for (int i = 0; i < len; i++)
		{
			IValue v1 = this.arguments.get(i);
			IValue v2 = v1.foldConstants();
			if (v1 != v2)
			{
				this.arguments.set(i, v2);
			}
		}
		return this;
	}
	
	@Override
	public boolean resolve(IContext context, IContext context1)
	{
		IType[] types = this.getTypes();
		if (types == null)
		{
			return false;
		}
		
		MethodMatch match = context.resolveMethod(context1, this.qualifiedName, types);
		if (match != null)
		{
			this.method = match.theMethod;
			return true;
		}
		return false;
	}
	
	@Override
	public IAccess resolve2(IContext context, IContext context1)
	{
		if (this.isSugarCall)
		{
			if (this.arguments.isEmpty())
			{
				FieldMatch f = context.resolveField(context1, this.qualifiedName);
				if (f != null)
				{
					FieldAccess access = new FieldAccess(this.position, this.instance, this.qualifiedName);
					access.field = f.theField;
					return access;
				}
			}
		}
		else
		{
			if (this.instance == null)
			{
				FieldMatch f = context.resolveField(context1, this.qualifiedName);
				if (f != null)
				{
					FieldAccess access = new FieldAccess(this.position, null, this.qualifiedName);
					access.field = f.theField;
					MethodCall call = new MethodCall(this.position, access, "apply");
					call.arguments = this.arguments;
					if (call.resolve(access.getType(), context1))
					{
						return call;
					}
				}
			}
		}
		
		return null;
	}
	
	@Override
	public IAccess resolve3(IContext context, IAccess next)
	{
		return null;
	}
	
	@Override
	public Marker getResolveError()
	{
		return new SemanticError(this.position, "'" + this.qualifiedName + "' could not be resolved to a method or field");
	}
	
	@Override
	public void writeExpression(MethodWriter visitor)
	{
		Annotation bytecode = this.method.getAnnotation(Type.ABytecode);
		
		// Writes the prefix opcodes if a @Bytecode annotation is present.
		if (bytecode != null)
		{
			visitBytecodeAnnotation(visitor, bytecode, "prefixOpcode", "prefixOpcodes");
		}
		
		// Writes the instance (the first operand).
		if (this.instance != null)
		{
			this.instance.writeExpression(visitor);
		}
		
		// Writes the infix opcodes if a @Bytecode annotation is present.
		if (bytecode != null)
		{
			visitBytecodeAnnotation(visitor, bytecode, "infixOpcode", "infixOpcodes");
		}
		
		// Writes the arguments (the second operand).
		for (IValue arg : this.arguments)
		{
			arg.writeExpression(visitor);
		}
		
		// Writes the postfix opcodes if a @Bytecode annotation is present.
		if (bytecode != null)
		{
			visitBytecodeAnnotation(visitor, bytecode, "postfixOpcode", "postfixOpcodes");
			return;
		}
		
		// If no @Bytecode annotation is present, write a normal invocation.
		IClass ownerClass = this.method.getTheClass();
		String owner = ownerClass.getInternalName();
		String name = this.method.getName();
		String desc = this.method.getDescriptor();
		int opcode;
		if (this.method.hasModifier(Modifiers.STATIC))
		{
			opcode = Opcodes.INVOKESTATIC;
		}
		else if (ownerClass.hasModifier(Modifiers.INTERFACE_CLASS))
		{
			opcode = Opcodes.INVOKEINTERFACE;
		}
		else if (this.instance instanceof SuperValue)
		{
			opcode = Opcodes.INVOKESPECIAL;
		}
		else
		{
			opcode = Opcodes.INVOKEVIRTUAL;
		}
		
		visitor.visitMethodInsn(opcode, owner, name, desc, ownerClass.hasModifier(Modifiers.INTERFACE_CLASS));
	}
	
	private static void visitBytecodeAnnotation(MethodWriter writer, Annotation annotation, String key1, String key2)
	{
		ValueList array = (ValueList) annotation.getValue(key2);
		if (array != null)
		{
			for (IValue v : array.values)
			{
				if (v instanceof FieldAccess)
				{
					v = v.foldConstants();
				}
				writer.visitInsn(((IntValue) v).value);
			}
			return;
		}
		
		IntValue i = (IntValue) annotation.getValue(key1);
		if (i != null)
		{
			writer.visitInsn(i.value);
		}
	}
	
	@Override
	public void writeStatement(MethodWriter writer)
	{
		this.writeExpression(writer);
	}
	
	@Override
	public void writeJump(MethodWriter writer, Label label)
	{
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		if (this.instance != null)
		{
			this.instance.toString("", buffer);
			if (this.dotless && !Formatting.Method.useJavaFormat)
			{
				buffer.append(Formatting.Method.dotlessSeperator);
			}
			else
			{
				buffer.append('.');
			}
		}
		
		if (Formatting.Method.convertQualifiedNames)
		{
			buffer.append(this.qualifiedName);
		}
		else
		{
			buffer.append(this.name);
		}
		
		if (this.isSugarCall && !Formatting.Method.useJavaFormat)
		{
			if (!this.arguments.isEmpty())
			{
				buffer.append(Formatting.Method.sugarCallSeperator);
				this.arguments.get(0).toString("", buffer);
			}
		}
		else
		{
			Util.parametersToString(this.arguments, buffer, true);
		}
	}
}
