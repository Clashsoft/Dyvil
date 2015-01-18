package dyvil.tools.compiler.ast.expression;

import java.util.List;

import jdk.internal.org.objectweb.asm.Label;
import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.IASTNode;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.ITyped;
import dyvil.tools.compiler.ast.type.Type;
import dyvil.tools.compiler.ast.value.*;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.lexer.marker.Marker;

public interface IValue extends IASTNode, ITyped
{
	public static int	NULL				= 0;
	public static int	BOOLEAN				= 1;
	public static int	BYTE				= 2;
	public static int	SHORT				= 3;
	public static int	CHAR				= 4;
	public static int	INT					= 5;
	public static int	LONG				= 6;
	public static int	FLOAT				= 7;
	public static int	DOUBLE				= 8;
	public static int	STRING				= 9;
	public static int	ENUM				= 10;
	
	public static int	THIS				= 11;
	public static int	SUPER				= 12;
	
	public static int	VALUE_LIST			= 13;
	
	public static int	CLASS_ACCESS		= 16;
	public static int	FIELD_ACCESS		= 17;
	public static int	FIELD_ASSIGN		= 18;
	public static int	METHOD_CALL			= 19;
	public static int	CONSTRUCTOR_CALL	= 20;
	public static int	BOOLEAN_AND			= 21;
	public static int	BOOLEAN_OR			= 22;
	
	public static int	TUPLE				= 48;
	public static int	LAMBDA				= 49;
	public static int	BYTECODE			= 50;
	
	public static int	RETURN				= 64;
	public static int	IF					= 65;
	public static int	SWITCH				= 66;
	public static int	FOR					= 67;
	public static int	WHILE				= 68;
	public static int	DO_WHILE			= 69;
	
	public int getValueType();
	
	public static boolean isNumeric(int t1)
	{
		return t1 >= BYTE && t1 <= DOUBLE;
	}
	
	public default boolean isConstant()
	{
		return false;
	}
	
	@Override
	public IType getType();
	
	@Override
	public default void setType(IType type)
	{
	}
	
	public default Object toObject()
	{
		throw new UnsupportedOperationException();
	}
	
	public default boolean requireType(IType type)
	{
		return type.equals(Type.ANY) || Type.isSuperType(type, this.getType());
	}
	
	public void resolveTypes(List<Marker> markers, IContext context);
	
	public IValue resolve(List<Marker> markers, IContext context);
	
	public void check(List<Marker> markers, IContext context);
	
	public IValue foldConstants();
	
	// Compilation
	
	/**
	 * Writes this {@link IValue} to the given {@link MethodWriter}
	 * {@code writer} as an expression. That means that this element remains as
	 * the first element of the stack.
	 * 
	 * @param visitor
	 */
	public void writeExpression(MethodWriter writer);
	
	/**
	 * Writes this {@link IValue} to the given {@link MethodWriter}
	 * {@code writer} as a statement. That means that this element is removed
	 * from the stack.
	 * 
	 * @param writer
	 */
	public void writeStatement(MethodWriter writer);
	
	/**
	 * Writes this {@link IValue} to the given {@link MethodWriter}
	 * {@code writer} as a jump expression to the given {@link Label}
	 * {@code dest}. By default, this calls
	 * {@link #writeExpression(MethodWriter)} and then writes an
	 * {@link Opcodes#IFEQ IFEQ} instruction pointing to {@code dest}. That
	 * means the JVM would jump to {@code dest} if the current value on the
	 * stack equals {@code 0}.
	 * 
	 * @param writer
	 * @param dest
	 */
	public default void writeJump(MethodWriter writer, Label dest)
	{
		this.writeExpression(writer);
		writer.visitJumpInsn(Opcodes.IFEQ, dest);
	}
	
	public static IValue fromObject(Object o)
	{
		if (o == null)
		{
			return new NullValue();
		}
		Class c = o.getClass();
		if (c == Character.class)
		{
			return new CharValue((Character) o);
		}
		else if (c == Integer.class)
		{
			return new IntValue((Integer) o);
		}
		else if (c == Long.class)
		{
			return new LongValue((Long) o);
		}
		else if (c == Float.class)
		{
			return new FloatValue((Float) o);
		}
		else if (c == Double.class)
		{
			return new DoubleValue((Double) o);
		}
		else if (c == String.class)
		{
			return new StringValue((String) o);
		}
		else if (c == int[].class)
		{
			ValueList valueList = new ValueList(null, true);
			for (int i : (int[]) o)
			{
				valueList.addValue(new IntValue(i));
			}
			return valueList;
		}
		else if (c == long[].class)
		{
			ValueList valueList = new ValueList(null, true);
			for (long l : (long[]) o)
			{
				valueList.addValue(new LongValue(l));
			}
			return valueList;
		}
		else if (c == float[].class)
		{
			ValueList valueList = new ValueList(null, true);
			for (float f : (float[]) o)
			{
				valueList.addValue(new FloatValue(f));
			}
			return valueList;
		}
		else if (c == double[].class)
		{
			ValueList valueList = new ValueList(null, true);
			for (double d : (double[]) o)
			{
				valueList.addValue(new DoubleValue(d));
			}
			return valueList;
		}
		return null;
	}
}
