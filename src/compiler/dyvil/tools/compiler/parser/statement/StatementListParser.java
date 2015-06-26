package dyvil.tools.compiler.parser.statement;

import dyvil.tools.compiler.ast.access.FieldAssign;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.expression.IValued;
import dyvil.tools.compiler.ast.member.Name;
import dyvil.tools.compiler.ast.statement.FieldInitializer;
import dyvil.tools.compiler.ast.statement.Label;
import dyvil.tools.compiler.ast.statement.StatementList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.ITyped;
import dyvil.tools.compiler.lexer.marker.SyntaxError;
import dyvil.tools.compiler.lexer.token.IToken;
import dyvil.tools.compiler.parser.EmulatorParser;
import dyvil.tools.compiler.parser.IParserManager;
import dyvil.tools.compiler.parser.expression.ExpressionParser;
import dyvil.tools.compiler.parser.type.TypeParser;
import dyvil.tools.compiler.transform.Symbols;
import dyvil.tools.compiler.util.ParserUtil;

public final class StatementListParser extends EmulatorParser implements IValued, ITyped
{
	private static final int	EXPRESSION	= 1;
	private static final int	TYPE		= 2;
	private static final int	SEPARATOR	= 4;
	
	protected StatementList		statementList;
	
	private Name				label;
	
	private IType				type;
	
	public StatementListParser(StatementList valueList)
	{
		this.statementList = valueList;
		this.mode = EXPRESSION;
	}
	
	@Override
	public void reset()
	{
		this.mode = EXPRESSION;
		this.label = null;
		this.firstToken = null;
		this.tryParser = null;
		this.pm = null;
		this.type = null;
		this.parser = null;
	}
	
	@Override
	public void parse(IParserManager pm, IToken token) throws SyntaxError
	{
		int type = token.type();
		
		if (type == Symbols.CLOSE_CURLY_BRACKET)
		{
			if (this.firstToken != null)
			{
				pm.jump(this.firstToken);
				this.reset();
				pm.pushParser(new ExpressionParser(this));
				this.mode = 0;
				return;
			}
			
			pm.popParser(true);
			return;
		}
		
		if (this.mode == EXPRESSION)
		{
			if (ParserUtil.isIdentifier(type) && token.next().type() == Symbols.COLON)
			{
				this.label = token.nameValue();
				pm.skip();
				return;
			}
			
			this.firstToken = token;
			this.parser = this.tryParser = new TypeParser(this);
			this.pm = pm;
			this.mode = TYPE;
		}
		if (this.mode == TYPE)
		{
			if (ParserUtil.isIdentifier(type) && token.next().type() == Symbols.EQUALS)
			{
				if (this.type == null)
				{
					System.out.println();
				}
				
				if (this.type != null)
				{
					FieldInitializer fi = new FieldInitializer(token.raw(), token.nameValue(), this.type);
					pm.pushParser(new ExpressionParser(fi));
					this.statementList.addValue(fi);
				}
				else
				{
					FieldAssign fa = new FieldAssign(token.raw(), null, token.nameValue());
					pm.pushParser(new ExpressionParser(fa));
					this.statementList.addValue(fa);
				}
				
				this.reset();
				this.mode = SEPARATOR;
				pm.skip();
				return;
			}
			else if (this.tryParser == null)
			{
				pm.jump(this.firstToken);
				this.reset();
				pm.pushParser(new ExpressionParser(this));
				this.mode = SEPARATOR;
				return;
			}
			
			try
			{
				this.parser.parse(this, token);
			}
			catch (Throwable ex)
			{
				pm.jump(this.firstToken);
				this.reset();
				pm.pushParser(new ExpressionParser(this));
				this.mode = SEPARATOR;
			}
			
			return;
		}
		if (this.mode == SEPARATOR)
		{
			if (type == Symbols.SEMICOLON)
			{
				this.mode = EXPRESSION;
				return;
			}
			this.mode = EXPRESSION;
			if (token.prev().type() == Symbols.CLOSE_CURLY_BRACKET)
			{
				pm.reparse();
				return;
			}
			throw new SyntaxError(token, "Invalid Statement List - ';' expected");
		}
	}
	
	@Override
	public void setValue(IValue value)
	{
		if (this.label != null)
		{
			this.statementList.addValue(value, new Label(this.label, value));
			this.label = null;
		}
		else
		{
			this.statementList.addValue(value);
		}
	}
	
	@Override
	public void setType(IType type)
	{
		this.type = type;
	}
	
	// ----- Ignore -----
	
	@Override
	public IValue getValue()
	{
		return null;
	}
	
	@Override
	public IType getType()
	{
		return null;
	}
}
