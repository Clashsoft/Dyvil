package dyvil.reflect.types;

import dyvil.lang.Type;
import dyvil.lang.literal.TupleConvertible;

@TupleConvertible
public class TupleType implements Type
{
	protected Type[] types;
	protected Class  theClass;
	
	public static TupleType apply(Type... types)
	{
		return new TupleType(types);
	}
	
	public TupleType(Type... types)
	{
		this.types = types;
	}
	
	@Override
	public Class erasure()
	{
		if (this.theClass == null)
		{
			try
			{
				return this.theClass = Class.forName(this.qualifiedName());
			}
			catch (ClassNotFoundException ex)
			{
				return null;
			}
		}
		return this.theClass;
	}
	
	@Override
	public String name()
	{
		return "Tuple" + this.types.length;
	}
	
	@Override
	public String qualifiedName()
	{
		return "dyvil/tuple/Tuple" + this.types.length;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		this.toString(builder);
		return builder.toString();
	}
	
	@Override
	public void toString(StringBuilder builder)
	{
		builder.append('(');
		int len = this.types.length;
		if (len > 0)
		{
			this.types[0].toString(builder);
			for (int i = 1; i < len; i++)
			{
				builder.append(", ");
				this.types[i].toString(builder);
			}
		}
		builder.append(')');
	}
	
	@Override
	public void appendSignature(StringBuilder builder)
	{
		builder.append('L').append(this.qualifiedName()).append(';');
	}
	
	@Override
	public void appendGenericSignature(StringBuilder builder)
	{
		builder.append('L').append(this.qualifiedName());
		builder.append('<');
		int len = this.types.length;
		if (len > 0)
		{
			this.types[0].appendGenericSignature(builder);
			for (int i = 1; i < len; i++)
			{
				this.types[i].appendGenericSignature(builder);
			}
		}
		builder.append('>').append(';');
	}
}
