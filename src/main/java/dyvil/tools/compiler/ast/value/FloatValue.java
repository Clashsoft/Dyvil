package dyvil.tools.compiler.ast.value;

import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import dyvil.tools.compiler.CompilerState;
import dyvil.tools.compiler.ast.ASTNode;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.type.Type;

public class FloatValue extends ASTNode implements IValue
{
	public float	value;
	
	public FloatValue(String value)
	{
		this.value = Float.parseFloat(value);
	}
	
	public FloatValue(float value)
	{
		this.value = value;
	}
	
	@Override
	public boolean isConstant()
	{
		return true;
	}
	
	@Override
	public Type getType()
	{
		return Type.FLOAT;
	}
	
	@Override
	public FloatValue applyState(CompilerState state, IContext context)
	{
		return this;
	}
	
	@Override
	public void write(MethodVisitor visitor)
	{
		visitor.visitLdcInsn(Float.valueOf(this.value));
	}
	
	@Override
	public void writeJump(MethodVisitor visitor, Label label)
	{
		visitor.visitLdcInsn(Float.valueOf(this.value));
		visitor.visitLdcInsn(Float.valueOf(0F));
		visitor.visitJumpInsn(Opcodes.IFNE, label);
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append(this.value).append('F');
	}
}
