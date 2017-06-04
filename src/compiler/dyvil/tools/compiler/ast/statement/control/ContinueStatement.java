package dyvil.tools.compiler.ast.statement.control;

import dyvil.source.position.SourcePosition;
import dyvil.tools.compiler.ast.context.ILabelContext;
import dyvil.tools.compiler.ast.statement.loop.ILoop;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.parsing.marker.MarkerList;

public class ContinueStatement extends JumpStatement
{
	public ContinueStatement(SourcePosition position)
	{
		super(position);
	}

	@Override
	public int valueTag()
	{
		return CONTINUE;
	}

	@Override
	public void resolveStatement(ILabelContext context, MarkerList markers)
	{
		if (this.name == null)
		{
			this.label = context.getContinueLabel();
			if (this.label == null)
			{
				markers.add(Markers.semanticError(this.position, "continue.invalid"));
			}

			return;
		}

		this.label = context.resolveLabel(this.name);
		if (this.label == null)
		{
			markers.add(Markers.semanticError(this.position, "resolve.label", this.name));
			return;
		}

		if (!(this.label.value instanceof ILoop))
		{
			markers.add(Markers.semanticError(this.position, "continue.invalid.type", this.name));
			return;
		}

		this.label = ((ILoop) this.label.value).getContinueLabel();
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append("continue");
		if (this.name != null)
		{
			buffer.append(' ').append(this.name);
		}
	}
}
