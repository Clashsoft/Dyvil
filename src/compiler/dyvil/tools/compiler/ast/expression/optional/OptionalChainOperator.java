package dyvil.tools.compiler.ast.expression.optional;

import dyvil.annotation.internal.NonNull;
import dyvil.reflect.Opcodes;
import dyvil.tools.asm.Label;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.compound.NullableType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.transform.TypeChecker;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.parsing.marker.MarkerList;

public class OptionalChainOperator extends OptionalUnwrapOperator implements IValue, OptionalChainAware
{
	// Metadata
	protected Label         elseLabel;

	public OptionalChainOperator(IValue receiver)
	{
		super(receiver);
	}

	@Override
	public int valueTag()
	{
		return OPTIONAL_CHAIN;
	}

	@Override
	public boolean needsOptionalElseLabel()
	{
		return this.elseLabel == null;
	}

	@Override
	public Label getOptionalElseLabel()
	{
		return this.elseLabel;
	}

	@Override
	public boolean setOptionalElseLabel(Label label, boolean top)
	{
		this.receiver.setOptionalElseLabel(label, false);
		this.elseLabel = label;
		return true;
	}

	@Override
	protected TypeChecker.MarkerSupplier getMarkerSupplier()
	{
		return TypeChecker.markerSupplier("optional.chain.type.incompatible");
	}

	@Override
	public void check(MarkerList markers, IContext context)
	{
		super.check(markers, context);

		if (this.elseLabel == null)
		{
			markers.add(Markers.semanticError(this.position, "optional.chain.invalid"));
		}
	}

	@Override
	public void toString(@NonNull String indent, @NonNull StringBuilder buffer)
	{
		this.receiver.toString(indent, buffer);
		buffer.append('?');
	}

	@Override
	public void writeNullCheckedExpression(MethodWriter writer, IType type) throws BytecodeException
	{
		this.writeExpression(writer, type);
	}

	@Override
	public void writeExpression(MethodWriter writer, IType type) throws BytecodeException
	{
		this.receiver.writeExpression(writer, type == null ? null : NullableType.unapply(type));

		if (this.elseLabel == null)
		{
			return;
		}

		final int varIndex = writer.localCount();

		writer.visitInsn(Opcodes.DUP);
		writer.visitVarInsn(Opcodes.ASTORE, varIndex);
		writer.visitJumpInsn(Opcodes.IFNULL, this.elseLabel);
		writer.visitVarInsn(Opcodes.ALOAD, varIndex);
	}
}
