package dyvil.tools.compiler.ast.statement.control;

import dyvil.reflect.Opcodes;
import dyvil.source.position.SourcePosition;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.header.IClassCompilableList;
import dyvil.tools.compiler.ast.header.ICompilableList;
import dyvil.tools.compiler.ast.statement.IStatement;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;

public abstract class JumpStatement implements IStatement
{
	protected Name name;

	// Metadata
	protected SourcePosition position;
	protected Label          label;

	public JumpStatement(SourcePosition position)
	{
		this.position = position;
	}

	@Override
	public SourcePosition getPosition()
	{
		return this.position;
	}

	@Override
	public void setPosition(SourcePosition position)
	{
		this.position = position;
	}

	public Name getName()
	{
		return this.name;
	}

	public void setName(Name name)
	{
		this.name = name;
	}

	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
	}

	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		return this;
	}

	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
	}

	@Override
	public void check(MarkerList markers, IContext context)
	{
	}

	@Override
	public IValue foldConstants()
	{
		return this;
	}

	@Override
	public IValue cleanup(ICompilableList compilableList, IClassCompilableList classCompilableList)
	{
		return this;
	}

	@Override
	public void writeStatement(MethodWriter writer) throws BytecodeException
	{
		writer.visitJumpInsn(Opcodes.GOTO, this.label.getTarget());
	}
}
