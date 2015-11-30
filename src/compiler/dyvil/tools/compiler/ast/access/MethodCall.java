package dyvil.tools.compiler.ast.access;

import dyvil.tools.compiler.DyvilCompiler;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.member.INamed;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.operator.Operators;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.transform.ConstantFolder;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public final class MethodCall extends AbstractCall implements INamed
{
	protected Name    name;
	protected boolean dotless;
	
	public MethodCall(ICodePosition position)
	{
		this.position = position;
	}
	
	public MethodCall(ICodePosition position, IValue instance, Name name)
	{
		this.position = position;
		this.receiver = instance;
		this.name = name;
	}
	
	public MethodCall(ICodePosition position, IValue instance, Name name, IArguments arguments)
	{
		this.position = position;
		this.receiver = instance;
		this.name = name;
		this.arguments = arguments;
	}
	
	public MethodCall(ICodePosition position, IValue instance, IMethod method, IArguments arguments)
	{
		this.position = position;
		this.receiver = instance;
		this.name = method.getName();
		this.method = method;
		this.arguments = arguments;
	}
	
	@Override
	public int valueTag()
	{
		return METHOD_CALL;
	}
	
	@Override
	public void setName(Name name)
	{
		this.name = name;
	}
	
	@Override
	public Name getName()
	{
		return this.name;
	}
	
	public boolean isDotless()
	{
		return this.dotless;
	}
	
	public void setDotless(boolean dotless)
	{
		this.dotless = dotless;
	}
	
	@Override
	public IValue toConstant(MarkerList markers)
	{
		int depth = DyvilCompiler.maxConstantDepth;
		IValue v = this;
		
		do
		{
			if (depth-- < 0)
			{
				return null;
			}
			
			v = v.foldConstants();
		}
		while (!v.isConstant());
		
		return v.toConstant(markers);
	}
	
	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		if (this.genericData != null)
		{
			this.genericData.resolveTypes(markers, context);
		}
		
		super.resolveTypes(markers, context);
	}
	
	@Override
	public IValue resolveCall(MarkerList markers, IContext context)
	{
		int args = this.arguments.size();
		if (args == 1 && this.receiver != null)
		{
			// Prioritized Infix Operators
			IValue op = Operators.getInfix_Priority(this.receiver, this.name, this.arguments.getFirstValue());
			if (op != null)
			{
				op.setPosition(this.position);
				return op;
			}
		}

		// Normal Method Resolution
		IMethod method = ICall.resolveMethod(context, this.receiver, this.name, this.arguments);
		if (method != null)
		{
			this.method = method;
			this.checkArguments(markers, context);
			return this;
		}
		
		if (args == 1)
		{
			IValue op;
			if (this.receiver == null)
			{
				// Prefix Operators
				op = Operators.getPrefix(this.name, this.arguments.getFirstValue());
				if (op != null)
				{
					op.setPosition(this.position);
					return op;
				}
			}
			else
			{
				// Infix Operators
				op = Operators.get(this.receiver, this.name, this.arguments.getFirstValue());
				if (op != null)
				{
					op.setPosition(this.position);
					return op;
				}

				String qualified = this.name.qualified;
				if (qualified.endsWith("$eq"))
				{
					Name name = Util.stripEq(this.name);

					return CompoundCall
							.resolveCall(markers, context, this.position, this.receiver, name, this.arguments);
				}
			}
		}
		if (args == 0 && this.receiver != null)
		{
			IValue op = Operators.getPostfix(this.receiver, this.name);
			if (op != null)
			{
				return op;
			}
		}
		
		// Resolve Apply Method
		if (this.receiver == null)
		{
			return ApplyMethodCall.resolveApply(markers, context, position, receiver, name, arguments, genericData);
		}
		
		return null;
	}
	
	@Override
	public void reportResolve(MarkerList markers, IContext context)
	{
		ICall.addResolveMarker(markers, this.position, this.receiver, this.name, this.arguments);
	}
	
	@Override
	public IValue foldConstants()
	{
		if (!this.arguments.isEmpty())
		{
			if (this.receiver != null)
			{
				if (this.receiver.isConstant())
				{
					IValue argument;
					if (this.arguments.size() == 1 && (argument = this.arguments.getFirstValue()).isConstant())
					{
						IValue folded = ConstantFolder.apply(this.receiver, this.name, argument);
						if (folded != null)
						{
							return folded;
						}
					}
				}
				else
				{
					this.receiver = this.receiver.foldConstants();
				}
			}
			this.arguments.foldConstants();
			return this;
		}
		
		if (this.receiver != null)
		{
			// Prefix methods are transformed to postfix notation
			if (this.receiver.isConstant())
			{
				IValue folded = ConstantFolder.apply(this.name, this.receiver);
				if (folded != null)
				{
					return folded;
				}
			}
			
			this.receiver = this.receiver.foldConstants();
		}
		return this;
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		if (this.receiver != null)
		{
			this.receiver.toString(prefix, buffer);
			if (this.dotless && !Formatting.getBoolean("method.access.java_format"))
			{
				buffer.append(' ');
			}
			else if (this.genericData == null)
			{
				buffer.append('.');
			}
		}
		
		if (this.genericData != null)
		{
			this.genericData.toString(prefix, buffer);
		}
		
		buffer.append(this.name);
		
		this.arguments.toString(prefix, buffer);
	}
}
