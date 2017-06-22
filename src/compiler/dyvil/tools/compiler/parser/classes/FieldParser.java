package dyvil.tools.compiler.parser.classes;

import dyvil.reflect.Modifiers;
import dyvil.source.position.SourcePosition;
import dyvil.tools.compiler.ast.annotation.AnnotationList;
import dyvil.tools.compiler.ast.consumer.IMemberConsumer;
import dyvil.tools.compiler.ast.consumer.ITypeConsumer;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.field.IProperty;
import dyvil.tools.compiler.ast.modifiers.ModifierList;
import dyvil.tools.compiler.ast.modifiers.ModifierSet;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.parser.ParserUtil;
import dyvil.tools.compiler.parser.expression.ExpressionParser;
import dyvil.tools.compiler.parser.type.TypeParser;
import dyvil.tools.compiler.transform.DyvilKeywords;
import dyvil.tools.compiler.transform.DyvilSymbols;
import dyvil.tools.parsing.IParserManager;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.lexer.BaseSymbols;
import dyvil.tools.parsing.lexer.Tokens;
import dyvil.tools.parsing.token.IToken;

public class FieldParser<T extends IDataMember> extends AbstractMemberParser implements ITypeConsumer
{
	protected static final int DECLARATOR = 0;
	protected static final int NAME       = 1;
	protected static final int TYPE       = 2;
	protected static final int VALUE      = 3;
	protected static final int PROPERTY   = 4;

	// Flags

	public static final int NO_VALUES     = 1;
	public static final int NO_PROPERTIES = 2;

	protected IMemberConsumer<T> consumer;
	private   int                flags;

	private T         dataMember;
	private IProperty property;

	private SourcePosition position;
	private Name           name;
	private IType type = Types.UNKNOWN;

	public FieldParser(IMemberConsumer<T> consumer)
	{
		this.consumer = consumer;
		this.modifiers = new ModifierList();
		// this.mode = DECLARATOR;
	}

	public FieldParser(IMemberConsumer<T> consumer, ModifierSet modifiers, AnnotationList annotations)
	{
		this.consumer = consumer;
		this.modifiers = modifiers;
		this.annotations = annotations;
		// this.mode = DECLARATOR;
	}

	public FieldParser<T> withFlags(int flags)
	{
		this.flags |= flags;
		return this;
	}

	@Override
	public void parse(IParserManager pm, IToken token)
	{
		final int type = token.type();
		switch (this.mode)
		{
		case DECLARATOR:
			switch (type)
			{
			case DyvilSymbols.AT:
				this.parseAnnotation(pm, token);
				return;
			case DyvilKeywords.ENUM:
				if (token.next().type() != DyvilKeywords.CONST)
				{
					break;
				}
				// enum const
				pm.skip();
				this.getModifiers().addIntModifier(Modifiers.ENUM_CONST);
				this.mode = NAME;
				return;
			case DyvilKeywords.CONST:
				this.getModifiers().addIntModifier(Modifiers.CONST);
				this.mode = NAME;
				return;
			case DyvilKeywords.LET:
				this.getModifiers().addIntModifier(Modifiers.FINAL);
				// Fallthrough
			case DyvilKeywords.VAR:
				this.mode = NAME;
				return;
			}

			if (this.parseModifier(pm, token))
			{
				return;
			}
			// Fallthrough
		case NAME:
			if (!ParserUtil.isIdentifier(type))
			{
				pm.report(token, "variable.identifier");
				return;
			}

			this.position = token.raw();
			this.name = token.nameValue();

			this.mode = TYPE;
			return;
		case TYPE:
			if (type == BaseSymbols.COLON)
			{
				// ... IDENTIFIER : TYPE ...
				pm.pushParser(new TypeParser(this));
				this.mode = VALUE;
				return;
			}
			// Fallthrough
		case VALUE:
			if ((this.flags & NO_VALUES) == 0 && type == BaseSymbols.EQUALS)
			{
				// definitely a field
				final T field = this.initField();
				if ((this.flags & NO_PROPERTIES) != 0)
				{
					this.mode = END;
					pm.pushParser(new ExpressionParser(field));
				}
				else
				{
					this.mode = PROPERTY;
					pm.pushParser(new ExpressionParser(field).withFlags(ExpressionParser.IGNORE_CLOSURE));
				}
				return;
			}
			// Fallthrough
		case PROPERTY:
			if ((this.flags & NO_PROPERTIES) == 0 && type == BaseSymbols.OPEN_CURLY_BRACKET)
			{
				// either a standalone property or a field property
				pm.pushParser(new PropertyBodyParser(this.initProperty()), true);
				this.mode = END;
				return;
			}
			// Fallthrough
		case END:
			if (this.property != null)
			{
				this.consumer.addProperty(this.property);
			}
			else
			{
				// for fields without values or properties, the dataMember field has not beed initialized yet
				this.consumer.addDataMember(this.initField());
			}
			pm.popParser(type != Tokens.EOF);
		}
	}

	private IProperty initProperty()
	{
		final IProperty prop;
		if (this.dataMember == null)
		{
			prop = this.consumer
				       .createProperty(this.position, this.name, this.type, this.getModifiers(), this.annotations);
			this.property = prop;
		}
		else
		{
			prop = this.dataMember.createProperty();
		}
		return prop;
	}

	private T initField()
	{
		if (this.dataMember != null)
		{
			return this.dataMember;
		}
		return this.dataMember = this.consumer.createDataMember(this.position, this.name, this.type, this.modifiers,
		                                                        this.annotations);
	}

	@Override
	public void setType(IType type)
	{
		this.type = type;
	}
}
