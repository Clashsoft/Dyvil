package dyvil.tools.compiler.ast.statement.exception;

import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.context.CombiningContext;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.context.IDefaultContext;
import dyvil.tools.compiler.ast.context.ILabelContext;
import dyvil.tools.compiler.ast.expression.AbstractValue;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.IType.TypePosition;
import dyvil.tools.compiler.ast.type.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.util.MarkerMessages;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.marker.Marker;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public final class TryStatement extends AbstractValue implements IDefaultContext
{
	protected IValue action;
	protected CatchBlock[] catchBlocks = new CatchBlock[1];
	protected int    catchBlockCount;
	protected IValue finallyBlock;
	
	// Metadata
	private IType commonType;
	
	public TryStatement(ICodePosition position)
	{
		this.position = position;
	}
	
	@Override
	public int valueTag()
	{
		return TRY;
	}

	@Override
	public boolean isResolved()
	{
		if (!this.action.isResolved())
		{
			return false;
		}
		for (int i = 0; i < this.catchBlockCount; i++)
		{
			if (!this.catchBlocks[i].action.isResolved())
			{
				return false;
			}
		}
		return true;
	}
	
	public void setAction(IValue action)
	{
		this.action = action;
	}
	
	public IValue getAction()
	{
		return this.action;
	}
	
	public void setFinallyBlock(IValue finallyBlock)
	{
		this.finallyBlock = finallyBlock;
	}
	
	public IValue getFinallyBlock()
	{
		return this.finallyBlock;
	}
	
	@Override
	public IType getType()
	{
		if (this.commonType != null)
		{
			return this.commonType;
		}

		if (this.action == null)
		{
			return Types.UNKNOWN;
		}

		IType combinedType = this.action.getType();
		for (int i = 0; i < this.catchBlockCount; i++)
		{
			final IType catchBlockType = this.catchBlocks[i].action.getType();
			combinedType = Types.combine(combinedType, catchBlockType);
			if (combinedType == null)
			{
				return this.commonType = Types.ANY;
			}
		}
		return this.commonType = combinedType;
	}
	
	@Override
	public IValue withType(IType type, ITypeContext typeContext, MarkerList markers, IContext context)
	{
		if (this.action != null)
		{
			final IValue typedAction = this.action.withType(type, typeContext, markers, context);
			if (typedAction == null)
			{
				final Marker marker = MarkerMessages
						.createError(this.action.getPosition(), "try.action.type.incompatible");
				marker.addInfo(MarkerMessages.getMarker("action.type", this.action.getType().toString()));
				marker.addInfo(MarkerMessages.getMarker("type.expected", type));
				markers.add(marker);
			}
			else
			{
				this.action = typedAction;
			}
		}

		for (int i = 0; i < this.catchBlockCount; i++)
		{
			final CatchBlock block = this.catchBlocks[i];
			final IValue action = block.action;
			final IValue typedAction = action.withType(type, typeContext, markers, context);

			if (typedAction == null)
			{
				final Marker marker = MarkerMessages.createError(action.getPosition(), "try.catch.type.incompatible");
				marker.addInfo(MarkerMessages.getMarker("try.catchblock.type", action.getType().toString()));
				marker.addInfo(MarkerMessages.getMarker("type.expected", type));
				markers.add(marker);
			}
			else
			{
				block.action = typedAction;
			}
		}

		this.commonType = type;
		return this;
	}
	
	@Override
	public boolean isType(IType type)
	{
		if (type == Types.VOID)
		{
			return true;
		}
		if (this.action != null && !this.action.isType(type))
		{
			return false;
		}
		for (int i = 0; i < this.catchBlockCount; i++)
		{
			if (!this.catchBlocks[i].action.isType(type))
			{
				return false;
			}
		}
		return true;
	}
	
	@Override
	public float getTypeMatch(IType type)
	{
		float total = this.action.getTypeMatch(type);
		if (total <= 0F)
		{
			return 0F;
		}
		for (int i = 0; i < this.catchBlockCount; i++)
		{
			final float blockMatch = this.catchBlocks[i].action.getTypeMatch(type);
			if (blockMatch <= 0F)
			{
				return 0F;
			}
			total += blockMatch;
		}

		return total / (1 + this.catchBlockCount);
	}
	
	public void addCatchBlock(CatchBlock block)
	{
		int index = this.catchBlockCount++;
		if (index >= this.catchBlocks.length)
		{
			CatchBlock[] temp = new CatchBlock[this.catchBlockCount];
			System.arraycopy(this.catchBlocks, 0, temp, 0, this.catchBlocks.length);
			this.catchBlocks = temp;
		}
		
		this.catchBlocks[index] = block;
	}
	
	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		if (this.action != null)
		{
			this.action.resolveTypes(markers, context);
		}
		
		for (int i = 0; i < this.catchBlockCount; i++)
		{
			CatchBlock block = this.catchBlocks[i];
			block.type = block.type.resolveType(markers, context);
			block.action.resolveTypes(markers, context);
		}
		
		if (this.finallyBlock != null)
		{
			this.finallyBlock.resolveTypes(markers, context);
		}
	}
	
	@Override
	public void resolveStatement(ILabelContext context, MarkerList markers)
	{
		if (this.action != null)
		{
			this.action.resolveStatement(context, markers);
		}
		for (int i = 0; i < this.catchBlockCount; i++)
		{
			this.catchBlocks[i].action.resolveStatement(context, markers);
		}
		
		if (this.finallyBlock != null)
		{
			this.finallyBlock.resolveStatement(context, markers);
		}
	}
	
	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		if (this.action != null)
		{
			this.action = this.action.resolve(markers, context);
		}
		
		for (int i = 0; i < this.catchBlockCount; i++)
		{
			final CatchBlock block = this.catchBlocks[i];
			block.type.resolve(markers, context);
			block.action = block.action.resolve(markers, new CombiningContext(block, context));
		}
		
		if (this.finallyBlock != null)
		{
			this.finallyBlock = this.finallyBlock.resolve(markers, context);

			final IValue typedFinally = this.finallyBlock.withType(Types.VOID, Types.VOID, markers, context);
			if (typedFinally == null)
			{
				final Marker marker = MarkerMessages
						.createError(this.finallyBlock.getPosition(), "try.finally.type.invalid");
				marker.addInfo(MarkerMessages.getMarker("try.finally.type", this.finallyBlock.getType().toString()));
				markers.add(marker);
			}
			else
			{
				this.finallyBlock = typedFinally;
			}
		}

		if (this.commonType != Types.VOID) {
		}
		return this;
	}
	
	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		if (this.action != null)
		{
			this.action.checkTypes(markers, new CombiningContext(this, context));
		}
		
		for (int i = 0; i < this.catchBlockCount; i++)
		{
			CatchBlock block = this.catchBlocks[i];
			block.type.checkType(markers, context, TypePosition.RETURN_TYPE);
			block.action.checkTypes(markers, new CombiningContext(block, context));
		}
		
		if (this.finallyBlock != null)
		{
			this.finallyBlock.checkTypes(markers, context);
		}
	}
	
	@Override
	public void check(MarkerList markers, IContext context)
	{
		if (this.action != null)
		{
			this.action.check(markers, new CombiningContext(this, context));
		}
		
		for (int i = 0; i < this.catchBlockCount; i++)
		{
			CatchBlock block = this.catchBlocks[i];
			block.type.check(markers, context);
			
			if (!Types.THROWABLE.isSuperTypeOf(block.type))
			{
				Marker marker = MarkerMessages.createMarker(block.position, "try.catch.type.not_throwable");
				marker.addInfo(MarkerMessages.getMarker("exception.type", block.type));
				markers.add(marker);
			}
			
			block.action.check(markers, context);
		}
		
		if (this.finallyBlock != null)
		{
			this.finallyBlock.check(markers, context);
		}
	}
	
	@Override
	public IValue foldConstants()
	{
		if (this.action != null)
		{
			this.action = this.action.foldConstants();
		}
		
		for (int i = 0; i < this.catchBlockCount; i++)
		{
			CatchBlock block = this.catchBlocks[i];
			block.type.foldConstants();
			block.action = block.action.foldConstants();
		}
		
		if (this.finallyBlock != null)
		{
			this.finallyBlock = this.finallyBlock.foldConstants();
		}
		return this;
	}
	
	@Override
	public IValue cleanup(IContext context, IClassCompilableList compilableList)
	{
		if (this.action != null)
		{
			this.action = this.action.cleanup(context, compilableList);
		}
		
		for (int i = 0; i < this.catchBlockCount; i++)
		{
			CatchBlock block = this.catchBlocks[i];
			block.type.cleanup(context, compilableList);
			block.action = block.action.cleanup(new CombiningContext(block, context), compilableList);
		}
		
		if (this.finallyBlock != null)
		{
			this.finallyBlock = this.finallyBlock.cleanup(context, compilableList);
		}
		return this;
	}
	
	@Override
	public boolean handleException(IType type)
	{
		for (int i = 0; i < this.catchBlockCount; i++)
		{
			if (this.catchBlocks[i].type.isSuperTypeOf(type))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public void writeExpression(MethodWriter writer, IType type) throws BytecodeException
	{
		if (type == null)
		{
			type = this.getType();
		}

		final boolean expression;
		final int storeInsn;
		final int localIndex;

		if (type != Types.VOID)
		{
			storeInsn = type.getStoreOpcode();
			localIndex = writer.localCount();
			expression = true;
		}
		else
		{
			storeInsn = 0;
			localIndex = -1;
			expression = false;
		}

		final dyvil.tools.asm.Label tryStart = new dyvil.tools.asm.Label();
		final dyvil.tools.asm.Label tryEnd = new dyvil.tools.asm.Label();
		final dyvil.tools.asm.Label endLabel = new dyvil.tools.asm.Label();
		
		writer.writeTargetLabel(tryStart);
		if (this.action != null)
		{
			this.action.writeExpression(writer, type);
			if (expression)
			{
				writer.writeVarInsn(storeInsn, localIndex);
				writer.resetLocals(localIndex);
			}

			writer.writeJumpInsn(Opcodes.GOTO, endLabel);
		}
		writer.writeLabel(tryEnd);
		
		for (int i = 0; i < this.catchBlockCount; i++)
		{
			final CatchBlock block = this.catchBlocks[i];
			final dyvil.tools.asm.Label handlerLabel = new dyvil.tools.asm.Label();
			final String handlerType = block.type.getInternalName();
			
			writer.writeTargetLabel(handlerLabel);
			writer.startCatchBlock(handlerType);
			
			// Check if the block's variable is actually used
			if (block.variable != null)
			{
				// If yes register a new local variable for the exception and
				// store it.
				final int localCount = writer.localCount();
				block.variable.writeInit(writer, null);
				block.action.writeExpression(writer, type);
				writer.resetLocals(localCount);
			}
			// Otherwise pop the exception from the stack
			else
			{
				writer.writeInsn(Opcodes.POP);
				block.action.writeExpression(writer, type);
			}

			if (expression)
			{
				writer.writeVarInsn(storeInsn, localIndex);
				writer.resetLocals(localIndex);
			}
			
			writer.writeCatchBlock(tryStart, tryEnd, handlerLabel, handlerType);
			writer.writeJumpInsn(Opcodes.GOTO, endLabel);
		}
		
		if (this.finallyBlock != null)
		{
			final dyvil.tools.asm.Label finallyLabel = new dyvil.tools.asm.Label();
			
			writer.writeLabel(finallyLabel);
			writer.startCatchBlock("java/lang/Throwable");
			writer.writeInsn(Opcodes.POP);

			writer.writeLabel(endLabel);
			this.finallyBlock.writeExpression(writer, Types.VOID);
			writer.writeFinallyBlock(tryStart, tryEnd, finallyLabel);
		}
		else
		{
			writer.writeLabel(endLabel);
		}

		if (expression)
		{
			writer.setLocalType(localIndex, type.getFrameType());

			writer.writeVarInsn(type.getLoadOpcode(), localIndex);
			writer.resetLocals(localIndex);
		}
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append("try");

		if (this.action != null && !Util.formatStatementList(prefix, buffer, this.action))
		{
			String actionPrefix = Formatting.getIndent("try.indent", prefix);
			if (Formatting.getBoolean("try.newline_after"))
			{
				buffer.append('\n').append(actionPrefix);
			}
			else if (Formatting.getBoolean("try.space_after"))
			{
				buffer.append(' ');
			}

			this.action.toString(actionPrefix, buffer);
		}

		for (int i = 0; i < this.catchBlockCount; i++)
		{
			CatchBlock block = this.catchBlocks[i];

			if (Formatting.getBoolean("try.catch.newline_before"))
			{
				buffer.append('\n').append(prefix);
			}

			buffer.append("catch");

			Formatting.appendSeparator(buffer, "try.catch.open_paren", '(');

			block.type.toString(prefix, buffer);
			buffer.append(' ').append(block.varName);

			if (Formatting.getBoolean("try.catch.close_paren.space_before"))
			{
				buffer.append(' ');
			}
			buffer.append(')');

			if (Util.formatStatementList(prefix, buffer, block.action))
			{
				continue;
			}

			String actionIndent = Formatting.getIndent("try.catch.indent", prefix);
			if (Formatting.getBoolean("try.catch.close_paren.newline_after"))
			{
				buffer.append('\n').append(actionIndent);
			}
			else if (Formatting.getBoolean("try.catch.close_paren.space_after"))
			{
				buffer.append(' ');
			}

			block.action.toString(prefix, buffer);
		}
		if (this.finallyBlock != null)
		{
			if (Formatting.getBoolean("try.finally.newline_before"))
			{
				buffer.append('\n').append(prefix);
			}

			buffer.append("finally");

			if (Util.formatStatementList(prefix, buffer, this.finallyBlock))
			{
				return;
			}

			String actionIndent = Formatting.getIndent("try.finally.indent", prefix);
			if (Formatting.getBoolean("try.finally.newline_after"))
			{
				buffer.append('\n').append(actionIndent);
			}
			else
			{
				buffer.append(' ');
			}

			this.finallyBlock.toString(prefix, buffer);
		}
	}
}
