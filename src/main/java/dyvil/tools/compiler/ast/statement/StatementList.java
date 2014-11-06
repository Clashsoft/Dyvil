package dyvil.tools.compiler.ast.statement;

import java.util.HashMap;
import java.util.Map;

import dyvil.tools.compiler.CompilerState;
import dyvil.tools.compiler.ast.api.IField;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.expression.ValueList;
import dyvil.tools.compiler.ast.field.FieldMatch;
import dyvil.tools.compiler.ast.method.MethodMatch;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.type.Type;
import dyvil.tools.compiler.ast.value.IValue;
import dyvil.tools.compiler.lexer.position.ICodePosition;

public class StatementList extends ValueList implements IStatement, IContext
{
	private IContext			context;
	private int					resolveIndex;
	
	public Map<String, IField>	variables	= new HashMap();
	
	public StatementList(ICodePosition position)
	{
		super(position);
	}
	
	public void addStatement(IStatement statement)
	{
		this.values.add(statement);
	}
	
	@Override
	public IValue applyState(CompilerState state, IContext context)
	{
		int len = this.values.size();
		if (state == CompilerState.FOLD_CONSTANTS)
		{
			if (len == 1)
			{
				return this.values.get(0);
			}
		}
		else if (state == CompilerState.RESOLVE)
		{
			for (IValue v : this.values)
			{
				if (!(v instanceof FieldAssign))
				{
					continue;
				}
				
				FieldAssign assign = (FieldAssign) v;
				if (!assign.initializer)
				{
					continue;
				}
				
				this.variables.put(assign.qualifiedName, assign.field);
			}
		}
		
		this.context = context;
		for (int i = 0; i < len; i++)
		{
			this.resolveIndex = i;
			IValue v = this.values.get(i);
			this.values.set(i, v.applyState(state, this));
		}
		return this;
	}
	
	@Override
	public boolean isStatic()
	{
		return this.context.isStatic();
	}
	
	@Override
	public Type getThisType()
	{
		return this.context.getThisType();
	}
	
	@Override
	public IClass resolveClass(String name)
	{
		return this.context.resolveClass(name);
	}
	
	@Override
	public FieldMatch resolveField(IContext context, String name)
	{
		IField field = this.variables.get(name);
		if (field != null)
		{
			return new FieldMatch(field, 1);
		}
		
		return this.context.resolveField(context, name);
	}
	
	@Override
	public MethodMatch resolveMethod(IContext context, String name, Type... argumentTypes)
	{
		return this.context.resolveMethod(context, name, argumentTypes);
	}
}
