package dyvil.tools.compiler.parser.classes;

import dyvil.tools.compiler.ast.annotation.Annotation;
import dyvil.tools.compiler.ast.annotation.AnnotationList;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.consumer.IClassBodyConsumer;
import dyvil.tools.compiler.ast.consumer.ITypeConsumer;
import dyvil.tools.compiler.ast.field.Field;
import dyvil.tools.compiler.ast.field.IField;
import dyvil.tools.compiler.ast.field.IProperty;
import dyvil.tools.compiler.ast.field.Property;
import dyvil.tools.compiler.ast.member.IMember;
import dyvil.tools.compiler.ast.method.*;
import dyvil.tools.compiler.ast.modifiers.*;
import dyvil.tools.compiler.ast.parameter.IParameterList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.parser.IParserManager;
import dyvil.tools.compiler.parser.Parser;
import dyvil.tools.compiler.parser.method.ExceptionListParser;
import dyvil.tools.compiler.parser.method.ParameterListParser;
import dyvil.tools.compiler.parser.statement.StatementListParser;
import dyvil.tools.compiler.parser.type.TypeParameterListParser;
import dyvil.tools.compiler.transform.DyvilKeywords;
import dyvil.tools.compiler.transform.DyvilSymbols;
import dyvil.tools.compiler.util.ParserUtil;
import dyvil.tools.parsing.lexer.BaseSymbols;
import dyvil.tools.parsing.token.IToken;

public final class ClassBodyParser extends Parser implements ITypeConsumer
{
	protected static final int END            = 0;
	protected static final int TYPE           = 1;
	protected static final int NAME_OPERATOR  = 2;
	protected static final int NAME           = 4;
	protected static final int GENERICS_END   = 8;
	protected static final int PARAMETERS     = 16;
	protected static final int PARAMETERS_END = 32;
	protected static final int FIELD_END      = 64;
	protected static final int PROPERTY_END   = 128;
	protected static final int METHOD_VALUE   = 256;
	protected static final int METHOD_END     = 512;
	
	protected IClass             theClass;
	protected IClassBodyConsumer consumer;
	
	private IType type;
	private ModifierSet modifiers = new ModifierList();
	private AnnotationList annotations;
	
	private IMember member;
	
	public ClassBodyParser(IClass theClass, IClassBodyConsumer consumer)
	{
		this.theClass = theClass;
		this.consumer = consumer;
		this.mode = TYPE;
	}
	
	public ClassBodyParser(IClassBodyConsumer consumer)
	{
		this.consumer = consumer;
		this.mode = TYPE;
	}
	
	private void reset()
	{
		this.mode = TYPE;
		this.modifiers = new ModifierList();
		this.annotations = null;
		this.type = null;
		this.member = null;
	}
	
	@Override
	public void parse(IParserManager pm, IToken token)
	{
		final int type = token.type();
		
		switch (this.mode)
		{
		case TYPE:
			switch (type)
			{
			case END:
				// no error
				pm.popParser();
				return;
			case BaseSymbols.CLOSE_CURLY_BRACKET:
				pm.popParser(true);
				return;
			case BaseSymbols.SEMICOLON:
				if (token.isInferred())
				{
					return;
				}
				this.reset();
				return;
			case DyvilKeywords.NEW:
				if (this.theClass == null)
				{
					this.mode = TYPE;
					pm.report(token, "Cannot define a constructor in this context");
					return;
				}
				Constructor c = new Constructor(token.raw(), this.theClass, this.modifiers);
				c.setAnnotations(this.annotations);
				this.member = c;
				this.mode = PARAMETERS;
				return;
			}
			Modifier modifier;
			if ((modifier = BaseModifiers.parseModifier(token, pm)) != null)
			{
				this.modifiers.addModifier(modifier);
				return;
			}

			int classType;
			if ((classType = ModifierUtil.readClassTypeModifier(token, pm)) >= 0)
			{
				if (this.theClass == null)
				{
					this.reset();
					pm.report(token, "class.invalid");
					return;
				}

				this.modifiers.addIntModifier(classType);
				ClassDeclarationParser parser = new ClassDeclarationParser(this.theClass, this.modifiers,
				                                                           this.annotations);
				pm.pushParser(parser);
				this.reset();
				return;
			}
			if (type == DyvilSymbols.AT)
			{
				if (this.annotations == null)
				{
					this.annotations = new AnnotationList();
				}
				
				Annotation annotation = new Annotation(token.raw());
				this.annotations.addAnnotation(annotation);
				pm.pushParser(pm.newAnnotationParser(annotation));
				return;
			}
			pm.pushParser(pm.newTypeParser(this), true);
			this.mode = NAME_OPERATOR;
			return;
		case NAME_OPERATOR:
			if (type == DyvilKeywords.OPERATOR)
			{
				this.mode = NAME;
				return;
			}
			// Fallthrough
		case NAME:
			if (!ParserUtil.isIdentifier(type))
			{
				this.reset();
				pm.report(token, "member.identifier");
				return;
			}

			final IToken nextToken = token.next();
			final int nextType = nextToken.type();

			switch (nextType)
			{
			case BaseSymbols.SEMICOLON:
			case BaseSymbols.CLOSE_CURLY_BRACKET:
			{
				final IField field = new Field(token.raw(), this.theClass, token.nameValue(), this.type,
				                               this.modifiers);
				field.setAnnotations(this.annotations);
				this.consumer.addField(field);

				if (type == BaseSymbols.CLOSE_CURLY_BRACKET)
				{
					pm.popParser(true);
					return;
				}

				pm.skip();
				this.reset();
				return;
			}
			case BaseSymbols.OPEN_PARENTHESIS:
			{

				final IMethod method = new CodeMethod(token.raw(), this.theClass, token.nameValue(), this.type,
				                                      this.modifiers);
				method.setAnnotations(this.annotations);
				this.mode = PARAMETERS;
				this.member = method;
				return;
			}
			case BaseSymbols.OPEN_CURLY_BRACKET:
				final Property property = new Property(token.raw(), this.theClass, token.nameValue(), this.type,
				                                       this.modifiers);
				property.setAnnotations(this.annotations);
				this.member = property;
				this.mode = PROPERTY_END;

				pm.skip();
				pm.pushParser(new PropertyParser(property));
				return;
			case BaseSymbols.EQUALS:
			{
				final IField field = new Field(token.raw(), this.theClass, token.nameValue(), this.type,
				                               this.modifiers);
				field.setAnnotations(this.annotations);
				this.member = field;
				this.mode = FIELD_END;

				pm.skip();
				pm.pushParser(pm.newExpressionParser(field));
				return;
			}
			case BaseSymbols.OPEN_SQUARE_BRACKET:
			{
				CodeMethod m = new CodeMethod(token.raw(), this.theClass, token.nameValue(), this.type, this.modifiers);
				m.setAnnotations(this.annotations);
				this.member = m;

				this.mode = GENERICS_END;
				pm.skip();
				pm.pushParser(new TypeParameterListParser(m));
				return;
			}
			}
			
			this.mode = TYPE;
			pm.report(token, "class.body.declaration.invalid");
			return;
		case GENERICS_END:
			this.mode = PARAMETERS;
			if (type != BaseSymbols.CLOSE_SQUARE_BRACKET)
			{
				pm.reparse();
				pm.report(token, "method.generic.close_bracket");
			}
			return;
		case PARAMETERS:
			this.mode = PARAMETERS_END;
			if (type == BaseSymbols.OPEN_PARENTHESIS)
			{
				pm.pushParser(new ParameterListParser((IParameterList) this.member));
				return;
			}
			pm.reparse();
			pm.report(token, "method.parameters.open_paren");
			return;
		case PARAMETERS_END:
			this.mode = METHOD_VALUE;
			if (type != BaseSymbols.CLOSE_PARENTHESIS)
			{
				pm.reparse();
				pm.report(token, "method.parameters.close_paren");
			}

			return;
		case METHOD_VALUE:
			if (type == BaseSymbols.CLOSE_CURLY_BRACKET)
			{
				this.consumer.addMethod((IMethod) this.member);
				pm.popParser(true);
				return;
			}
			if (type == BaseSymbols.SEMICOLON)
			{
				this.consumer.addMethod((IMethod) this.member);
				this.reset();
				return;
			}

			if (parseMethodBody(pm, type, (ICallableMember) this.member))
			{
				this.mode = METHOD_END;
				return;
			}

			pm.reparse();
			pm.report(token, "method.body.separator");
			this.mode = TYPE;
			return;
		case METHOD_END:
			if (this.member instanceof IMethod)
			{
				this.consumer.addMethod((IMethod) this.member);
			}
			else
			{
				this.consumer.addConstructor((IConstructor) this.member);
			}
			pm.reparse();
			this.reset();
			return;
		case FIELD_END:
			this.consumer.addField((IField) this.member);
			pm.reparse();
			this.reset();
			return;
		case PROPERTY_END:
			this.consumer.addProperty((IProperty) this.member);
			pm.reparse();
			this.reset();
			return;
		}
	}

	public static boolean parseMethodBody(IParserManager pm, int type, ICallableMember member)
	{
		if (type == BaseSymbols.OPEN_CURLY_BRACKET)
		{
			pm.pushParser(new StatementListParser(member), true);
			return true;
		}
		if (type == BaseSymbols.EQUALS)
		{
			pm.pushParser(pm.newExpressionParser(member));
			return true;
		}
		if (type == DyvilKeywords.THROWS)
		{
			pm.pushParser(new ExceptionListParser(member));
			return true;
		}
		return false;
	}
	
	@Override
	public void setType(IType type)
	{
		this.type = type;
	}
}
