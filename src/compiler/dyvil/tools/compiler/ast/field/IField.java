package dyvil.tools.compiler.ast.field;

import dyvil.tools.compiler.ast.IASTNode;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.expression.IValued;
import dyvil.tools.compiler.ast.member.IClassCompilable;
import dyvil.tools.compiler.ast.member.IMember;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;

public interface IField extends IASTNode, IMember, IClassCompilable, IValued
{
	public default boolean isEnumConstant()
	{
		return false;
	}
	
	public default boolean isField()
	{
		return false;
	}
	
	public default boolean isVariable()
	{
		return true;
	}
	
	// Compilation
	
	public void writeGet(MethodWriter writer, IValue instance) throws BytecodeException;
	
	public void writeSet(MethodWriter writer, IValue instance, IValue value) throws BytecodeException;
	
	public String getDescription();
	
	public String getSignature();
}
