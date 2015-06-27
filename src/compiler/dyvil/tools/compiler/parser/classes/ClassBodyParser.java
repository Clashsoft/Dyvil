package dyvil.tools.compiler.parser.classes;

import java.lang.annotation.ElementType;

import dyvil.tools.compiler.ast.annotation.Annotation;
import dyvil.tools.compiler.ast.classes.CodeClass;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.consumer.IClassBodyConsumer;
import dyvil.tools.compiler.ast.consumer.ITypeConsumer;
import dyvil.tools.compiler.ast.consumer.IValueConsumer;
import dyvil.tools.compiler.ast.field.Field;
import dyvil.tools.compiler.ast.field.IField;
import dyvil.tools.compiler.ast.field.IProperty;
import dyvil.tools.compiler.ast.field.Property;
import dyvil.tools.compiler.ast.member.IAnnotationList;
import dyvil.tools.compiler.ast.member.IMember;
import dyvil.tools.compiler.ast.member.Name;
import dyvil.tools.compiler.ast.method.Constructor;
import dyvil.tools.compiler.ast.method.IExceptionList;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.Method;
import dyvil.tools.compiler.ast.parameter.IParameterList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.lexer.marker.SyntaxError;
import dyvil.tools.compiler.lexer.token.IToken;
import dyvil.tools.compiler.parser.IParserManager;
import dyvil.tools.compiler.parser.Parser;
import dyvil.tools.compiler.parser.annotation.AnnotationParser;
import dyvil.tools.compiler.parser.expression.ExpressionParser;
import dyvil.tools.compiler.parser.method.ExceptionListParser;
import dyvil.tools.compiler.parser.method.ParameterListParser;
import dyvil.tools.compiler.parser.type.TypeParser;
import dyvil.tools.compiler.parser.type.TypeVariableListParser;
import dyvil.tools.compiler.transform.Keywords;
import dyvil.tools.compiler.transform.Symbols;
import dyvil.tools.compiler.util.ModifierTypes;
import dyvil.tools.compiler.util.ParserUtil;

public final class ClassBodyParser extends Parser implements ITypeConsumer, IAnnotationList
{
	public static final int			TYPE			= 1;
	public static final int			NAME			= 2;
	public static final int			GENERICS_END	= 4;
	public static final int			PARAMETERS		= 8;
	public static final int			PARAMETERS_END	= 16;
	public static final int			FIELD_END		= 32;
	public static final int			PROPERTY_END	= 64;
	public static final int			METHOD_VALUE	= 128;
	public static final int			METHOD_END		= 256;
	
	protected IClass				theClass;
	protected IClassBodyConsumer	consumer;
	
	private IType					type;
	private int						modifiers;
	private Annotation[]			annotations		= new Annotation[2];
	private int						annotationCount;
	
	private IMember					member;
	
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
	
	@Override
	public void reset()
	{
		this.mode = TYPE;
		this.modifiers = 0;
		this.annotationCount = 0;
		this.type = null;
		this.member = null;
	}
	
	@Override
	public void parse(IParserManager pm, IToken token) throws SyntaxError
	{
		int type = token.type();
		
		if (type == Symbols.CLOSE_CURLY_BRACKET)
		{
			pm.popParser(true);
			return;
		}
		
		switch (this.mode)
		{
		case TYPE:
			if (type == Symbols.SEMICOLON)
			{
				if (token.isInferred())
				{
					return;
				}
				
				this.reset();
				return;
			}
			if (type == Keywords.NEW)
			{
				if (this.theClass == null)
				{
					this.mode = 0;
					throw new SyntaxError(token, "Cannot define a constructor in this context");
				}
				
				Constructor c = new Constructor(this.theClass);
				this.consumer.addConstructor(c);
				c.position = token.raw();
				c.modifiers = this.modifiers;
				c.setAnnotations(this.annotations, this.annotationCount);
				this.member = c;
				
				this.mode = PARAMETERS;
				return;
			}
			int i = 0;
			if ((i = ModifierTypes.MEMBER.parse(type)) != -1)
			{
				this.modifiers |= i;
				return;
			}
			if ((i = ModifierTypes.CLASS_TYPE.parse(type)) != -1)
			{
				if (this.theClass == null)
				{
					this.mode = 0;
					throw new SyntaxError(token, "Cannot define a class in this context");
				}
				
				CodeClass codeClass = new CodeClass(null, this.theClass.getUnit(), this.modifiers);
				codeClass.setAnnotations(this.getAnnotations(), this.annotationCount);
				codeClass.setOuterClass(this.theClass);
				codeClass.setModifiers(this.modifiers);
				
				ClassDeclarationParser parser = new ClassDeclarationParser(this.theClass.getBody(), codeClass);
				pm.pushParser(parser, true);
				this.reset();
				return;
			}
			if (token.nameValue() == Name.at)
			{
				Annotation annotation = new Annotation(token.raw());
				this.addAnnotation(annotation);
				pm.pushParser(new AnnotationParser(annotation));
				return;
			}
			pm.pushParser(new TypeParser(this), true);
			this.mode = NAME;
			return;
		case NAME:
			if (!ParserUtil.isIdentifier(type))
			{
				this.reset();
				throw new SyntaxError(token, "Invalid Member Declaration - Name expected", true);
			}
			IToken next = token.next();
			type = next.type();
			if (type == Symbols.SEMICOLON)
			{
				Field f = new Field(this.theClass, token.nameValue(), this.type);
				f.position = token.raw();
				f.modifiers = this.modifiers;
				f.setAnnotations(this.getAnnotations(), this.annotationCount);
				this.consumer.addField(f);
				
				pm.skip();
				this.reset();
				return;
			}
			if (type == Symbols.OPEN_PARENTHESIS)
			{
				this.mode = PARAMETERS;
				
				Method m = new Method(this.theClass, token.nameValue(), this.type);
				m.modifiers = this.modifiers;
				m.position = token.raw();
				m.setAnnotations(this.getAnnotations(), this.annotationCount);
				this.member = m;
				return;
			}
			if (type == Symbols.OPEN_CURLY_BRACKET)
			{
				Property p = new Property(this.theClass, token.nameValue(), this.type);
				p.position = token.raw();
				p.modifiers = this.modifiers;
				p.setAnnotations(this.getAnnotations(), this.annotationCount);
				this.member = p;
				this.mode = FIELD_END;
				
				pm.skip();
				pm.pushParser(new PropertyParser(p));
				return;
			}
			if (type == Symbols.EQUALS)
			{
				Field f = new Field(this.theClass, token.nameValue(), this.type);
				f.position = token.raw();
				f.modifiers = this.modifiers;
				f.setAnnotations(this.getAnnotations(), this.annotationCount);
				this.member = f;
				this.mode = FIELD_END;
				
				pm.skip();
				pm.pushParser(new ExpressionParser(f));
				return;
			}
			if (type == Symbols.OPEN_SQUARE_BRACKET)
			{
				Method m = new Method(this.theClass, token.nameValue(), this.type);
				m.modifiers = this.modifiers;
				m.position = token.raw();
				m.setAnnotations(this.getAnnotations(), this.annotationCount);
				this.member = m;
				
				this.mode = GENERICS_END;
				pm.skip();
				pm.pushParser(new TypeVariableListParser(m));
				return;
			}
			return;
		case GENERICS_END:
			this.mode = PARAMETERS;
			if (type == Symbols.CLOSE_SQUARE_BRACKET)
			{
				return;
			}
			throw new SyntaxError(token, "Invalid Generic Type Parameter List - ']' expected", true);
		case PARAMETERS:
			this.mode = PARAMETERS_END;
			if (type == Symbols.OPEN_PARENTHESIS)
			{
				pm.pushParser(new ParameterListParser((IParameterList) this.member));
				return;
			}
			throw new SyntaxError(token, "Invalid Parameter List - '(' expected", true);
		case PARAMETERS_END:
			this.mode = METHOD_VALUE;
			if (type == Symbols.CLOSE_PARENTHESIS)
			{
				return;
			}
			throw new SyntaxError(token, "Invalid Parameter List - ')' expected", true);
		case METHOD_VALUE:
			if (type == Symbols.SEMICOLON)
			{
				this.consumer.addMethod((IMethod) this.member);
				this.reset();
				return;
			}
			this.mode = METHOD_END;
			if (type == Symbols.OPEN_CURLY_BRACKET)
			{
				pm.pushParser(new ExpressionParser((IValueConsumer) this.member), true);
				return;
			}
			if (type == Symbols.EQUALS)
			{
				pm.pushParser(new ExpressionParser((IValueConsumer) this.member));
				return;
			}
			if (type == Keywords.THROWS)
			{
				pm.pushParser(new ExceptionListParser((IExceptionList) this.member));
				return;
			}
			return;
		case METHOD_END:
			this.consumer.addMethod((IMethod) this.member);
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
	
	@Override
	public void setType(IType type)
	{
		this.type = type;
	}
	
	@Override
	public int annotationCount()
	{
		return this.annotationCount;
	}
	
	@Override
	public Annotation[] getAnnotations()
	{
		Annotation[] a = new Annotation[this.annotationCount];
		System.arraycopy(this.annotations, 0, a, 0, this.annotationCount);
		return a;
	}
	
	@Override
	public void addAnnotation(Annotation annotation)
	{
		int index = this.annotationCount++;
		if (this.annotationCount > this.annotations.length)
		{
			Annotation[] temp = new Annotation[this.annotationCount];
			System.arraycopy(this.annotations, 0, temp, 0, index);
			this.annotations = temp;
		}
		this.annotations[index] = annotation;
	}
	
	// Override Methods
	
	@Override
	public void setAnnotation(int index, Annotation annotation)
	{
	}
	
	@Override
	public void removeAnnotation(int index)
	{
	}
	
	@Override
	public Annotation getAnnotation(int index)
	{
		return null;
	}
	
	@Override
	public Annotation getAnnotation(IClass type)
	{
		return null;
	}
	
	@Override
	public ElementType getAnnotationType()
	{
		return null;
	}
}
