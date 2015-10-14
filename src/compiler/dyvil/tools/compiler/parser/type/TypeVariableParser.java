package dyvil.tools.compiler.parser.type;

import dyvil.tools.compiler.ast.generic.IGeneric;
import dyvil.tools.compiler.ast.generic.ITypeVariable;
import dyvil.tools.compiler.ast.generic.TypeVariable;
import dyvil.tools.compiler.ast.generic.Variance;
import dyvil.tools.compiler.ast.member.Name;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.ITyped;
import dyvil.tools.compiler.parser.IParserManager;
import dyvil.tools.compiler.parser.Parser;
import dyvil.tools.compiler.util.ParserUtil;
import dyvil.tools.parsing.token.IToken;

public final class TypeVariableParser extends Parser implements ITyped
{
	public static final int	NAME			= 1;
	public static final int	TYPE_VARIABLE	= 16;
	
	public static final int	UPPER	= 1;
	public static final int	LOWER	= 2;
	
	protected IGeneric generic;
	
	private byte			boundMode;
	private ITypeVariable	variable;
	
	public TypeVariableParser(IGeneric typed)
	{
		this.generic = typed;
		this.mode = NAME;
	}
	
	@Override
	public void parse(IParserManager pm, IToken token)
	{
		int type = token.type();
		if (this.mode == NAME)
		{
			if (ParserUtil.isIdentifier(type))
			{
				Name name = token.nameValue();
				if (name == Name.plus || name == Name.minus)
				{
					IToken next = token.next();
					if (ParserUtil.isIdentifier(next.type()))
					{
						Variance v = name == Name.minus ? Variance.CONTRAVARIANT : Variance.COVARIANT;
						this.variable = new TypeVariable(next, this.generic, next.nameValue(), v);
						this.mode = TYPE_VARIABLE;
						pm.skip();
						return;
					}
				}
				
				this.variable = new TypeVariable(token, this.generic, token.nameValue(), Variance.INVARIANT);
				this.mode = TYPE_VARIABLE;
				return;
			}
			pm.report(token, "Invalid Type Variable - Name expected");
			return;
		}
		if (this.mode == TYPE_VARIABLE)
		{
			if (ParserUtil.isTerminator(type))
			{
				if (this.variable != null)
				{
					this.generic.addTypeVariable(this.variable);
				}
				pm.popParser(true);
				return;
			}
			
			if (!ParserUtil.isIdentifier(type))
			{
				if (this.variable != null)
				{
					this.generic.addTypeVariable(this.variable);
				}
				pm.popParser(true);
				pm.report(token, "Invalid Type Variable - '>=', '<=' or '&' expected");
				return;
			}
			
			Name name = token.nameValue();
			if (this.boundMode == 0)
			{
				if (name == Name.gtcolon) // >: - Lower Bound
				{
					pm.pushParser(pm.newTypeParser(this));
					this.boundMode = LOWER;
					return;
				}
				if (name == Name.ltcolon) // <: - Upper Bounds
				{
					pm.pushParser(pm.newTypeParser(this));
					this.boundMode = UPPER;
					return;
				}
			}
			else if (this.boundMode == UPPER)
			{
				if (name == Name.amp)
				{
					pm.pushParser(pm.newTypeParser(this));
					return;
				}
			}
			this.generic.addTypeVariable(this.variable);
			pm.popParser(true);
			return;
		}
	}
	
	@Override
	public void setType(IType type)
	{
		if (this.boundMode == UPPER)
		{
			this.variable.addUpperBound(type);
		}
		else if (this.boundMode == LOWER)
		{
			this.variable.setLowerBound(type);
		}
	}
	
	@Override
	public IType getType()
	{
		return null;
	}
}
