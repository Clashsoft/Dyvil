package dyvil.tools.compiler.ast.constructor;

import dyvil.reflect.Modifiers;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.member.Member;
import dyvil.tools.compiler.ast.modifiers.ModifierSet;
import dyvil.tools.compiler.ast.modifiers.ModifierUtil;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Types;
import dyvil.tools.compiler.backend.ClassWriter;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.transform.Names;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

import java.lang.annotation.ElementType;

public class Initializer extends Member implements IInitializer
{
	protected IValue value;

	public Initializer(ICodePosition position, ModifierSet modifiers)
	{
		super(position, Names.init, Types.VOID, modifiers);
	}

	@Override
	public IValue getValue()
	{
		return this.value;
	}

	@Override
	public void setValue(IValue value)
	{
		this.value = value;
	}

	@Override
	public ElementType getElementType()
	{
		return ElementType.CONSTRUCTOR;
	}

	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		super.resolveTypes(markers, context);

		if (this.value != null)
		{
			this.value.resolveTypes(markers, context);
		}
	}

	@Override
	public void resolve(MarkerList markers, IContext context)
	{
		super.resolve(markers, context);

		if (this.value != null)
		{
			this.value = this.value.resolve(markers, context);

			final IValue typed = IType.convertValue(this.value, Types.VOID, Types.VOID, markers, context);
			if (typed == null)
			{
				Util.createTypeError(markers, this.value, Types.VOID, Types.VOID, "initializer.type");
			}
			else
			{
				this.value = typed;
			}
		}
	}

	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		super.checkTypes(markers, context);

		if (this.value != null)
		{
			this.value.checkTypes(markers, context);
		}
	}

	@Override
	public void check(MarkerList markers, IContext context)
	{
		super.check(markers, context);

		ModifierUtil.checkModifiers(markers, this, this.modifiers, Modifiers.PRIVATE | Modifiers.STATIC);

		if (this.value != null)
		{
			this.value.check(markers, context);
		}
	}

	@Override
	public void foldConstants()
	{
		if (this.value != null)
		{
			this.value.foldConstants();
		}
	}

	@Override
	public void cleanup(IContext context, IClassCompilableList compilableList)
	{
		super.cleanup(context, compilableList);

		if (this.value != null)
		{
			this.value = this.value.cleanup(context, compilableList);
		}
	}

	@Override
	public void write(ClassWriter writer) throws BytecodeException
	{
		// done in writeInit / writeStaticInit
	}

	@Override
	public void writeInit(MethodWriter writer) throws BytecodeException
	{
		if (!this.modifiers.hasIntModifier(Modifiers.STATIC))
		//  ^ not
		{
			this.value.writeExpression(writer, Types.VOID);
		}
	}

	@Override
	public void writeStaticInit(MethodWriter writer) throws BytecodeException
	{
		if (this.modifiers.hasIntModifier(Modifiers.STATIC))
		{
			this.value.writeExpression(writer, Types.VOID);
		}
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		super.toString(prefix, buffer);
		buffer.append("init");

		if (this.value == null || Util.formatStatementList(prefix, buffer, this.value))
		{
			return;
		}

		buffer.append(' ');
		this.value.toString(prefix, buffer);
	}
}
