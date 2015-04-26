package dyvil.tools.compiler.ast.expression;

import java.util.List;

import dyvil.tools.compiler.ast.ASTNode;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.field.IField;
import dyvil.tools.compiler.ast.generic.ITypeVariable;
import dyvil.tools.compiler.ast.member.IMember;
import dyvil.tools.compiler.ast.member.Name;
import dyvil.tools.compiler.ast.method.ConstructorMatch;
import dyvil.tools.compiler.ast.method.MethodMatch;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.pattern.IPattern;
import dyvil.tools.compiler.ast.pattern.IPatterned;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.lexer.marker.MarkerList;
import dyvil.tools.compiler.lexer.position.ICodePosition;

public final class CaseStatement extends ASTNode implements IValue, IValued, IPatterned, IContext
{
	protected IPattern			pattern;
	protected IValue			condition;
	protected IValue			value;
	
	private transient IContext	context;
	
	public CaseStatement(ICodePosition position)
	{
		this.position = position;
	}
	
	@Override
	public int valueTag()
	{
		return CASE_STATEMENT;
	}
	
	@Override
	public IType getType()
	{
		return Types.UNKNOWN;
	}
	
	@Override
	public boolean isType(IType type)
	{
		return type == Types.VOID;
	}
	
	@Override
	public int getTypeMatch(IType type)
	{
		return 0;
	}
	
	@Override
	public void setValue(IValue value)
	{
		this.value = value;
	}
	
	@Override
	public IValue getValue()
	{
		return this.value;
	}
	
	public void setCondition(IValue condition)
	{
		this.condition = condition;
	}
	
	public IValue getCondition()
	{
		return this.condition;
	}
	
	@Override
	public void setPattern(IPattern pattern)
	{
		this.pattern = pattern;
	}
	
	@Override
	public IPattern getPattern()
	{
		return this.pattern;
	}
	
	// IContext
	
	@Override
	public boolean isStatic()
	{
		return this.context.isStatic();
	}
	
	@Override
	public IClass getThisClass()
	{
		return this.context.getThisClass();
	}
	
	@Override
	public Package resolvePackage(Name name)
	{
		return this.context.resolvePackage(name);
	}
	
	@Override
	public IClass resolveClass(Name name)
	{
		return this.context.resolveClass(name);
	}
	
	@Override
	public ITypeVariable resolveTypeVariable(Name name)
	{
		return this.context.resolveTypeVariable(name);
	}
	
	@Override
	public IField resolveField(Name name)
	{
		IField f = this.pattern.resolveField(name);
		if (f != null)
		{
			return f;
		}
		
		return this.context.resolveField(name);
	}
	
	@Override
	public void getMethodMatches(List<MethodMatch> list, IValue instance, Name name, IArguments arguments)
	{
		this.context.getMethodMatches(list, instance, name, arguments);
	}
	
	@Override
	public void getConstructorMatches(List<ConstructorMatch> list, IArguments arguments)
	{
		this.context.getConstructorMatches(list, arguments);
	}
	
	@Override
	public byte getAccessibility(IMember member)
	{
		return this.context.getAccessibility(member);
	}
	
	// Phases
	
	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		if (this.condition != null)
		{
			this.condition.resolveTypes(markers, context);
		}
		if (this.value != null)
		{
			this.value.resolveTypes(markers, context);
		}
	}
	
	@Override
	public CaseStatement resolve(MarkerList markers, IContext context)
	{
		this.context = context;
		if (this.condition != null)
		{
			this.condition = this.condition.resolve(markers, this);
		}
		if (this.value != null)
		{
			this.value = this.value.resolve(markers, this);
		}
		this.context = null;
		return this;
	}
	
	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		this.context = context;
		if (this.condition != null)
		{
			this.condition.checkTypes(markers, this);
		}
		if (this.value != null)
		{
			this.value.checkTypes(markers, this);
		}
		this.context = null;
	}
	
	@Override
	public void check(MarkerList markers, IContext context)
	{
		this.context = context;
		if (this.condition != null)
		{
			this.condition.check(markers, this);
		}
		if (this.value != null)
		{
			this.value.check(markers, this);
		}
		this.context = null;
	}
	
	@Override
	public CaseStatement foldConstants()
	{
		if (this.condition != null)
		{
			this.condition = this.condition.foldConstants();
		}
		if (this.value != null)
		{
			this.value = this.value.foldConstants();
		}
		return this;
	}
	
	@Override
	public void writeExpression(MethodWriter writer) throws BytecodeException
	{
	}
	
	@Override
	public void writeStatement(MethodWriter writer) throws BytecodeException
	{
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append("case ");
		if (this.pattern != null)
		{
			this.pattern.toString(prefix, buffer);
		}
		if (this.condition != null)
		{
			buffer.append(" if ");
			this.condition.toString(prefix, buffer);
		}
		buffer.append(" : ");
		if (this.value != null)
		{
			this.value.toString(prefix, buffer);
		}
	}
}
