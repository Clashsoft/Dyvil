package dyvil.tools.compiler.ast.field;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import dyvil.tools.compiler.CompilerState;
import dyvil.tools.compiler.ast.api.IField;
import dyvil.tools.compiler.ast.method.Member;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.type.Type;
import dyvil.tools.compiler.ast.value.IValue;
import dyvil.tools.compiler.lexer.position.ICodePosition;

public class Variable extends Member implements IField
{
	public int index;
	
	public Label start;
	public Label end;
	
	public Variable()
	{
		super(null);
	}
	
	public Variable(ICodePosition position, String name, Type type)
	{
		super(null, name, type, 0);
		this.position = position;
	}
	
	@Override
	public void setValue(IValue value)
	{}
	
	@Override
	public IValue getValue()
	{
		return null;
	}
	
	@Override
	public String getDescription()
	{
		return this.type.getExtendedName();
	}

	@Override
	public String getSignature()
	{
		return this.type.getSignature();
	}

	@Override
	public Variable applyState(CompilerState state, IContext context)
	{
		if (state == CompilerState.RESOLVE_TYPES)
		{
			this.type = this.type.resolve(context);
		}
		return this;
	}
	
	@Override
	public void write(ClassWriter writer)
	{}
	
	@Override
	public void writeGet(MethodVisitor visitor)
	{
		visitor.visitVarInsn(this.type.getLoadOpcode(), this.index);
	}
	
	@Override
	public void writeSet(MethodVisitor visitor)
	{
		visitor.visitVarInsn(this.type.getStoreOpcode(), this.index);
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		this.type.toString("", buffer);
		buffer.append(' ').append(this.name);
	}
}
