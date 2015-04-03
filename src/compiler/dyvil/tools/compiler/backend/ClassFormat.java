package dyvil.tools.compiler.backend;

import java.io.File;

import org.objectweb.asm.Opcodes;

import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.generic.IGeneric;
import dyvil.tools.compiler.ast.generic.TypeVariable;
import dyvil.tools.compiler.ast.generic.WildcardType;
import dyvil.tools.compiler.ast.member.Name;
import dyvil.tools.compiler.ast.method.IConstructor;
import dyvil.tools.compiler.ast.method.IMethodSignature;
import dyvil.tools.compiler.ast.type.*;

public final class ClassFormat
{
	public static File			javaRTJar;
	public static File			dyvilRTJar;
	
	public static final int		H_GETFIELD			= Opcodes.H_GETFIELD;
	public static final int		H_GETSTATIC			= Opcodes.H_GETSTATIC;
	public static final int		H_PUTFIELD			= Opcodes.H_PUTFIELD;
	public static final int		H_PUTSTATIC			= Opcodes.H_PUTSTATIC;
	public static final int		H_INVOKEVIRTUAL		= Opcodes.H_INVOKEVIRTUAL;
	public static final int		H_INVOKESTATIC		= Opcodes.H_INVOKESTATIC;
	public static final int		H_INVOKESPECIAL		= Opcodes.H_INVOKESPECIAL;
	public static final int		H_NEWINVOKESPECIAL	= Opcodes.H_NEWINVOKESPECIAL;
	public static final int		H_INVOKEINTERFACE	= Opcodes.H_INVOKEINTERFACE;
	
	public static final int		T_BOOLEAN			= 4;
	public static final int		T_CHAR				= 5;
	public static final int		T_FLOAT				= 6;
	public static final int		T_DOUBLE			= 7;
	public static final int		T_BYTE				= 8;
	public static final int		T_SHORT				= 9;
	public static final int		T_INT				= 10;
	public static final int		T_LONG				= 11;
	
	public static final Integer	UNINITIALIZED_THIS	= Opcodes.UNINITIALIZED_THIS;
	public static final Integer	NULL				= Opcodes.NULL;
	public static final Integer	TOP					= Opcodes.TOP;
	public static final Integer	BOOLEAN				= new Integer(1);
	public static final Integer	BYTE				= new Integer(1);
	public static final Integer	SHORT				= new Integer(1);
	public static final Integer	CHAR				= new Integer(1);
	public static final Integer	INT					= Opcodes.INTEGER;
	public static final Integer	LONG				= Opcodes.LONG;
	public static final Integer	FLOAT				= Opcodes.FLOAT;
	public static final Integer	DOUBLE				= Opcodes.DOUBLE;
	
	static
	{
		String s = System.getProperty("sun.boot.class.path");
		int index = s.indexOf("rt.jar");
		if (index != -1)
		{
			int index1 = s.lastIndexOf(':', index);
			int index2 = s.indexOf(':', index + 1);
			String s1 = s.substring(index1 + 1, index2);
			javaRTJar = new File(s1);
		}
		
		File bin = new File("bin");
		if (bin.exists())
		{
			dyvilRTJar = bin;
		}
		else
		{
			s = System.getenv("DYVIL_HOME");
			if (s == null || s.isEmpty())
			{
				throw new UnsatisfiedLinkError("No installed Dyvil Runtime Library found!");
			}
			dyvilRTJar = new File(s);
		}
	}
	
	public static String packageToInternal(String name)
	{
		return name.replace('.', '/');
	}
	
	public static String internalToPackage(String name)
	{
		return name.replace('/', '.');
	}
	
	public static String extendedToPackage(String name)
	{
		int len = name.length() - 1;
		StringBuilder builder = new StringBuilder(len - 1);
		for (int i = 1; i < len; i++)
		{
			char c = name.charAt(i);
			if (c == '/')
			{
				builder.append('.');
				continue;
			}
			builder.append(c);
		}
		return builder.toString();
	}
	
	public static String userToExtended(String name)
	{
		switch (name)
		{
		case "boolean":
			return "Z";
		case "byte":
			return "B";
		case "short":
			return "S";
		case "char":
			return "C";
		case "int":
			return "I";
		case "long":
			return "J";
		case "float":
			return "F";
		case "double":
			return "D";
		case "void":
			return "V";
		}
		if (name.length() > 1)
		{
			return "L" + name.replace('.', '/') + ";";
		}
		return name;
	}
	
	public static IType internalToType(String internal)
	{
		Type type = new Type();
		setInternalName(type, internal, 0, internal.length());
		return type;
	}
	
	public static IType extendedToType(String extended)
	{
		return readType(extended, 0, extended.length());
	}
	
	public static IType readReturnType(String desc) {
		return readType(desc, desc.lastIndexOf(')') + 1, desc.length());
	}
	
	public static void readClassSignature(String desc, IClass iclass)
	{
		int i = 0;
		if (desc.charAt(0) == '<')
		{
			i++;
			while (desc.charAt(i) != '>')
			{
				i = readGeneric(desc, i, iclass);
			}
			i++;
		}
		
		int len = desc.length();
		i = readTyped(desc, i, iclass);
		while (i < len)
		{
			i = readTypeList(desc, i, iclass);
		}
	}

	public static void readMethodType(String desc, IMethodSignature method)
	{
		int i = 1;
		if (desc.charAt(0) == '<')
		{
			while (desc.charAt(i) != '>')
			{
				i = readGeneric(desc, i, method);
			}
			i += 2;
		}
		while (desc.charAt(i) != ')')
		{
			i = readTypeList(desc, i, method);
		}
		i++;
		readTyped(desc, i, method);
	}

	public static void readConstructorType(String desc, IConstructor constructor)
	{
		int i = 1;
		while (desc.charAt(i) != ')')
		{
			i = readTypeList(desc, i, constructor);
		}
	}

	private static void setInternalName(IType type, String desc, int start, int end)
	{
		int index = desc.lastIndexOf('/', end);
		if (index < start)
		{
			// No slash in type name, skip internal -> package name conversion
			type.setName(Name.getQualified(desc.substring(start, end)));
			type.setFullName(desc.substring(start, end));
			return;
		}
		
		type.setName(Name.getQualified(desc.substring(index + 1, end)));
		StringBuilder buf = new StringBuilder(end - index + 1);
		for (; start < end; start++)
		{
			char c = desc.charAt(start);
			if (c == '/')
			{
				buf.append('.');
				continue;
			}
			buf.append(c);
		}
		type.setFullName(buf.toString());
	}

	private static IType readType(String desc, int start, int end)
	{
		int array = 0;
		while (desc.charAt(start) == '[')
		{
			array++;
			start++;
		}
		
		switch (desc.charAt(start))
		{
		case 'V':
			return Types.VOID.getArrayType(array);
		case 'Z':
			return Types.BOOLEAN.getArrayType(array);
		case 'B':
			return Types.BYTE.getArrayType(array);
		case 'S':
			return Types.SHORT.getArrayType(array);
		case 'C':
			return Types.CHAR.getArrayType(array);
		case 'I':
			return Types.INT.getArrayType(array);
		case 'J':
			return Types.LONG.getArrayType(array);
		case 'F':
			return Types.FLOAT.getArrayType(array);
		case 'D':
			return Types.DOUBLE.getArrayType(array);
		case 'L':
			return readReferenceType(desc, start + 1, end - 1);
		}
		return null;
	}
	
	private static IType readReferenceType(String desc, int start, int end)
	{
		int index = desc.indexOf('<', start);
		if (index != -1 && index < end)
		{
			GenericType type = new GenericType();
			setInternalName(type, desc, start, index);
			index++;
			
			while (desc.charAt(index) != '>')
			{
				index = readTypeList(desc, index, type);
			}
			return type;
		}
		
		IType type = new Type();
		setInternalName(type, desc, start, end);
		return type;
	}
	
	private static int readTyped(String desc, int start, ITyped typed)
	{
		int array = 0;
		char c;
		while ((c = desc.charAt(start)) == '[')
		{
			array++;
			start++;
		}
		
		switch (c)
		{
		case 'V':
			typed.setType(Types.VOID.getArrayType(array));
			return start + 1;
		case 'Z':
			typed.setType(Types.BOOLEAN.getArrayType(array));
			return start + 1;
		case 'B':
			typed.setType(Types.BYTE.getArrayType(array));
			return start + 1;
		case 'S':
			typed.setType(Types.SHORT.getArrayType(array));
			return start + 1;
		case 'C':
			typed.setType(Types.CHAR.getArrayType(array));
			return start + 1;
		case 'I':
			typed.setType(Types.INT.getArrayType(array));
			return start + 1;
		case 'J':
			typed.setType(Types.LONG.getArrayType(array));
			return start + 1;
		case 'F':
			typed.setType(Types.FLOAT.getArrayType(array));
			return start + 1;
		case 'D':
			typed.setType(Types.DOUBLE.getArrayType(array));
			return start + 1;
		case 'L':
		{
			int end1 = getMatchingSemicolon(desc, start, desc.length());
			IType type = readReferenceType(desc, start + 1, end1);
			type.setArrayDimensions(array);
			typed.setType(type);
			return end1 + 1;
		}
		case 'T':
		{
			int end1 = desc.indexOf(';', start);
			IType type = new Type(Name.getQualified(desc.substring(start + 1, end1)));
			
			type.setArrayDimensions(array);
			typed.setType(type);
			return end1 + 1;
		}
		case '*':
			typed.setType(new WildcardType());
			return start + 1;
		case '+':
		{
			int end1 = getMatchingSemicolon(desc, start, desc.length());
			WildcardType var = new WildcardType();
			var.addUpperBound(readType(desc, start + 1, end1));
			typed.setType(var);
			return end1 + 1;
		}
		case '-':
		{
			int end1 = getMatchingSemicolon(desc, start, desc.length());
			WildcardType var = new WildcardType();
			var.setLowerBound(readType(desc, start + 1, end1));
			typed.setType(var);
			return end1 + 1;
		}
		}
		return start;
	}
	
	private static int readTypeList(String desc, int start, ITypeList list)
	{
		int array = 0;
		char c;
		while ((c = desc.charAt(start)) == '[')
		{
			array++;
			start++;
		}
		
		switch (c)
		{
		case 'V':
			list.addType(Types.VOID.getArrayType(array));
			return start + 1;
		case 'Z':
			list.addType(Types.BOOLEAN.getArrayType(array));
			return start + 1;
		case 'B':
			list.addType(Types.BYTE.getArrayType(array));
			return start + 1;
		case 'C':
			list.addType(Types.CHAR.getArrayType(array));
			return start + 1;
		case 'S':
			list.addType(Types.SHORT.getArrayType(array));
			return start + 1;
		case 'I':
			list.addType(Types.INT.getArrayType(array));
			return start + 1;
		case 'J':
			list.addType(Types.LONG.getArrayType(array));
			return start + 1;
		case 'F':
			list.addType(Types.FLOAT.getArrayType(array));
			return start + 1;
		case 'D':
			list.addType(Types.DOUBLE.getArrayType(array));
			return start + 1;
		case 'L':
		{
			int end1 = getMatchingSemicolon(desc, start, desc.length());
			IType type = readReferenceType(desc, start + 1, end1);
			type.setArrayDimensions(array);
			list.addType(type);
			return end1 + 1;
		}
		case 'T':
		{
			int end1 = desc.indexOf(';', start);
			IType type = new Type(Name.getQualified(desc.substring(start + 1, end1)));
			type.setArrayDimensions(array);
			list.addType(type);
			return end1 + 1;
		}
		case '*':
			list.addType(new WildcardType());
			return start + 1;
		case '+':
		{
			int end1 = getMatchingSemicolon(desc, start, desc.length());
			WildcardType var = new WildcardType();
			var.addUpperBound(readType(desc, start + 1, end1));
			list.addType(var);
			return end1 + 1;
		}
		case '-':
		{
			int end1 = getMatchingSemicolon(desc, start, desc.length());
			WildcardType var = new WildcardType();
			var.setLowerBound(readType(desc, start + 1, end1));
			list.addType(var);
			return end1 + 1;
		}
		}
		return start;
	}
	
	private static int readGeneric(String desc, int start, IGeneric generic)
	{
		TypeVariable typeVar = new TypeVariable(generic);
		int index = desc.indexOf(':', start);
		typeVar.name = Name.getQualified(desc.substring(start, index));
		if (desc.charAt(index + 1) == ':')
		{
			index++;
			typeVar.addUpperBound(Types.OBJECT);
		}
		while (desc.charAt(index) == ':')
		{
			index = readTypeList(desc, index + 1, typeVar);
		}
		generic.addTypeVariable(typeVar);
		return index;
	}
	
	private static int getMatchingSemicolon(String s, int start, int end)
	{
		int depth = 0;
		for (int i = start; i < end; i++)
		{
			char c = s.charAt(i);
			if (c == '<')
			{
				depth++;
			}
			else if (c == '>')
			{
				depth--;
			}
			else if (c == ';' && depth == 0)
			{
				return i;
			}
		}
		return -1;
	}
}
