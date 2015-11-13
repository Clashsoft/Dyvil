package dyvil.reflect.types;

import dyvil.lang.Type;
import dyvil.lang.literal.NilConvertible;

import dyvil.annotation._internal.object;

@NilConvertible
public @object class UnknownType<T> implements Type<T>
{
	public static final UnknownType instance = new UnknownType();
	
	public static <T> UnknownType<T> apply()
	{
		return instance;
	}
	
	@Override
	public Class getTheClass()
	{
		return null;
	}
	
	@Override
	public String getName()
	{
		return "auto";
	}
	
	@Override
	public String getQualifiedName()
	{
		return "auto";
	}
	
	@Override
	public String toString()
	{
		return "auto";
	}
	
	@Override
	public void appendSignature(StringBuilder builder)
	{
		builder.append("Ljava/lang/Object;");
	}
}
