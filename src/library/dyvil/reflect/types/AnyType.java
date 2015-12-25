package dyvil.reflect.types;

import dyvil.annotation._internal.DyvilModifiers;
import dyvil.lang.Type;
import dyvil.lang.literal.NilConvertible;
import dyvil.reflect.Modifiers;

@NilConvertible
@DyvilModifiers(Modifiers.OBJECT_CLASS)
public class AnyType implements Type<Object>
{
	public static final AnyType instance = new AnyType();
	
	public static AnyType apply()
	{
		return instance;
	}
	
	@Override
	public Class<Object> erasure()
	{
		return null;
	}
	
	@Override
	public String name()
	{
		return "any";
	}
	
	@Override
	public String toString()
	{
		return "any";
	}
	
	@Override
	public String qualifiedName()
	{
		return "dyvil/lang/Any";
	}
	
	@Override
	public void appendSignature(StringBuilder builder)
	{
		builder.append("Ljava/lang/Object;");
	}
}
