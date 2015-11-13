package dyvil.tools.compiler.ast.type;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import dyvil.reflect.Opcodes;
import dyvil.tools.asm.TypeAnnotatableVisitor;
import dyvil.tools.asm.TypePath;
import dyvil.tools.compiler.ast.annotation.IAnnotation;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.constant.*;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeVariable;
import dyvil.tools.compiler.ast.method.ConstructorMatchList;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MethodMatchList;
import dyvil.tools.compiler.ast.parameter.EmptyArguments;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.reference.ReferenceType;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;

import static dyvil.reflect.Opcodes.*;

public final class PrimitiveType implements IType
{
	// Duplicate of this mapping is present in dyvil.reflect.types.PrimitiveType
	public static final int	VOID_CODE		= 0;
	public static final int	BOOLEAN_CODE	= 1;
	public static final int	BYTE_CODE		= 2;
	public static final int	SHORT_CODE		= 3;
	public static final int	CHAR_CODE		= 4;
	public static final int	INT_CODE		= 5;
	public static final int	LONG_CODE		= 6;
	public static final int	FLOAT_CODE		= 7;
	public static final int	DOUBLE_CODE		= 8;
	
	private static final long PROMOTION_BITS = 0x3C3C0CFC;
	
	static
	{
		// Code to generate the value of PROMOTION_BITS. Uncomment as needed.
		// @formatter:off
		/*
		long promoBits = 0L;
		promoBits |= bitMask(BYTE_CODE, SHORT_CODE) | bitMask(BYTE_CODE, CHAR_CODE) | bitMask(BYTE_CODE, INT_CODE);
		promoBits |= bitMask(SHORT_CODE, CHAR_CODE) | bitMask(SHORT_CODE, INT_CODE);
		// Integer types can be promoted to long, float and double
		for (int i = BYTE_CODE; i <= INT_CODE; i++)
		{
			promoBits |= bitMask(i, LONG_CODE);
			promoBits |= bitMask(i, FLOAT_CODE);
		}
		// Everything can be promoted to double
		for (int i = BYTE_CODE; i <= FLOAT_CODE; i++)
		{
			promoBits |= bitMask(i, DOUBLE_CODE);
		}
		PROMOTION_BITS = promoBits;
		*/
		// @formatter:on
	}
	
	protected final Name	name;
	protected IClass		theClass;
	
	private final int	typecode;
	private final char	typeChar;
	
	private final int		opcodeOffset1;
	private final int		opcodeOffset2;
	private final Object	frameType;
	
	protected IMethod	boxMethod;
	protected IMethod	unboxMethod;
	
	private IClass			arrayClass;
	private ReferenceType	refType;
	private IType			simpleRefType;
	
	public PrimitiveType(Name name, int typecode, char typeChar, int loadOpcode, int aloadOpcode, Object frameType)
	{
		this.name = name;
		this.typecode = typecode;
		this.typeChar = typeChar;
		this.opcodeOffset1 = loadOpcode - Opcodes.ILOAD;
		this.opcodeOffset2 = aloadOpcode - Opcodes.IALOAD;
		this.frameType = frameType;
	}
	
	public static IType getPrimitiveType(IType type)
	{
		if (type.isArrayType())
		{
			return type;
		}
		IClass iclass = type.getTheClass();
		if (iclass == Types.VOID_CLASS)
		{
			return Types.VOID;
		}
		if (iclass == Types.BOOLEAN_CLASS)
		{
			return Types.BOOLEAN;
		}
		if (iclass == Types.BYTE_CLASS)
		{
			return Types.BYTE;
		}
		if (iclass == Types.SHORT_CLASS)
		{
			return Types.SHORT;
		}
		if (iclass == Types.CHAR_CLASS)
		{
			return Types.CHAR;
		}
		if (iclass == Types.INT_CLASS)
		{
			return Types.INT;
		}
		if (iclass == Types.LONG_CLASS)
		{
			return Types.LONG;
		}
		if (iclass == Types.FLOAT_CLASS)
		{
			return Types.FLOAT;
		}
		if (iclass == Types.DOUBLE_CLASS)
		{
			return Types.DOUBLE;
		}
		return type;
	}
	
	public static PrimitiveType fromTypecode(int typecode)
	{
		switch (typecode)
		{
		case BOOLEAN_CODE:
			return Types.BOOLEAN;
		case BYTE_CODE:
			return Types.BYTE;
		case SHORT_CODE:
			return Types.SHORT;
		case CHAR_CODE:
			return Types.CHAR;
		case INT_CODE:
			return Types.INT;
		case LONG_CODE:
			return Types.LONG;
		case FLOAT_CODE:
			return Types.FLOAT;
		case DOUBLE_CODE:
			return Types.DOUBLE;
		default:
			return Types.VOID;
		}
	}
	
	@Override
	public int typeTag()
	{
		return PRIMITIVE;
	}
	
	@Override
	public boolean isPrimitive()
	{
		return true;
	}
	
	@Override
	public int getTypecode()
	{
		return this.typecode;
	}
	
	@Override
	public boolean isGenericType()
	{
		return false;
	}
	
	@Override
	public ITypeVariable getTypeVariable()
	{
		return null;
	}
	
	@Override
	public final IType getObjectType()
	{
		return new ClassType(this.theClass);
	}
	
	@Override
	public ReferenceType getRefType()
	{
		ReferenceType refType = this.refType;
		if (refType == null)
		{
			String className = this.theClass.getName().qualified + "Ref";
			return this.refType = new ReferenceType(Package.dyvilLangRef.resolveClass(className), this);
		}
		return refType;
	}
	
	@Override
	public IType getSimpleRefType()
	{
		IType refType = this.simpleRefType;
		if (refType == null)
		{
			String className = "Simple" + this.theClass.getName().qualified + "Ref";
			return this.simpleRefType = new ClassType(Package.dyvilLangRefSimple.resolveClass(className));
		}
		return refType;
	}
	
	@Override
	public IMethod getBoxMethod()
	{
		return this.boxMethod;
	}
	
	@Override
	public IMethod getUnboxMethod()
	{
		return this.unboxMethod;
	}
	
	@Override
	public boolean isArrayType()
	{
		return false;
	}
	
	@Override
	public int getArrayDimensions()
	{
		return 0;
	}
	
	@Override
	public IType getElementType()
	{
		return null;
	}
	
	@Override
	public IClass getArrayClass()
	{
		IClass iclass = this.arrayClass;
		if (iclass == null)
		{
			String className = this.theClass.getName().qualified + "Array";
			return this.arrayClass = Package.dyvilArray.resolveClass(className);
		}
		return iclass;
	}
	
	@Override
	public Name getName()
	{
		return this.name;
	}
	
	@Override
	public IClass getTheClass()
	{
		return this.theClass;
	}
	
	@Override
	public boolean isSuperTypeOf(IType type)
	{
		if (type == this)
		{
			return true;
		}
		if (type.typeTag() == WILDCARD_TYPE)
		{
			return type.isSameType(this);
		}
		if (type.isArrayType())
		{
			return false;
		}
		
		return this.isSuperClassOf(type);
	}
	
	@Override
	public boolean isSuperClassOf(IType that)
	{
		if (this.theClass == that.getTheClass())
		{
			return true;
		}
		
		if (that.isPrimitive())
		{
			return isPromotable(that.getTypecode(), this.typecode);
		}
		
		return false;
	}
	
	@Override
	public int getSuperTypeDistance(IType superType)
	{
		if (this.theClass == superType.getTheClass())
		{
			return 1;
		}
		if (superType.isArrayType())
		{
			return 0;
		}
		if (superType.isPrimitive() && isPromotable(this.typecode, superType.getTypecode()))
		{
			return 2;
		}
		int m = this.theClass.getSuperTypeDistance(superType);
		if (m <= 0)
		{
			return 0;
		}
		return m + 1;
	}
	
	private static int bitMask(int from, int to)
	{
		return 1 << (from | to << 3);
	}
	
	private static boolean isPromotable(int from, int to)
	{
		return (PROMOTION_BITS & bitMask(from, to)) != 0;
	}
	
	@Override
	public boolean classEquals(IType type)
	{
		return type == this;
	}
	
	@Override
	public IType resolveType(ITypeVariable typeVar)
	{
		return null;
	}
	
	@Override
	public void inferTypes(IType concrete, ITypeContext typeContext)
	{
	}
	
	@Override
	public boolean isResolved()
	{
		return true;
	}
	
	@Override
	public IType resolveType(MarkerList markers, IContext context)
	{
		return this;
	}
	
	@Override
	public void resolve(MarkerList markers, IContext context)
	{
	}
	
	@Override
	public void checkType(MarkerList markers, IContext context, TypePosition position)
	{
	}
	
	@Override
	public void check(MarkerList markers, IContext context)
	{
	}
	
	@Override
	public void foldConstants()
	{
	}
	
	@Override
	public void cleanup(IContext context, IClassCompilableList compilableList)
	{
	}
	
	@Override
	public boolean hasTypeVariables()
	{
		return false;
	}
	
	@Override
	public IType getConcreteType(ITypeContext context)
	{
		return this;
	}
	
	@Override
	public IDataMember resolveField(Name name)
	{
		return null;
	}
	
	@Override
	public void getMethodMatches(MethodMatchList list, IValue instance, Name name, IArguments arguments)
	{
		if (this.theClass != null)
		{
			this.theClass.getMethodMatches(list, instance, name, arguments);
		}
	}
	
	@Override
	public void getConstructorMatches(ConstructorMatchList list, IArguments arguments)
	{
	}
	
	@Override
	public IMethod getFunctionalMethod()
	{
		return null;
	}
	
	@Override
	public String getInternalName()
	{
		return this.theClass.getInternalName();
	}
	
	@Override
	public void appendExtendedName(StringBuilder buf)
	{
		buf.append(this.typeChar);
	}
	
	@Override
	public String getSignature()
	{
		return null;
	}
	
	@Override
	public void appendSignature(StringBuilder buf)
	{
		buf.append(this.typeChar);
	}
	
	@Override
	public int getLoadOpcode()
	{
		return Opcodes.ILOAD + this.opcodeOffset1;
	}
	
	@Override
	public int getArrayLoadOpcode()
	{
		return Opcodes.IALOAD + this.opcodeOffset2;
	}
	
	@Override
	public int getStoreOpcode()
	{
		return Opcodes.ISTORE + this.opcodeOffset1;
	}
	
	@Override
	public int getArrayStoreOpcode()
	{
		return Opcodes.IASTORE + this.opcodeOffset2;
	}
	
	@Override
	public int getReturnOpcode()
	{
		return Opcodes.IRETURN + this.opcodeOffset1;
	}
	
	@Override
	public Object getFrameType()
	{
		return this.frameType;
	}
	
	@Override
	public void writeTypeExpression(MethodWriter writer) throws BytecodeException
	{
		writer.writeLDC(this.typecode);
		writer.writeInvokeInsn(Opcodes.INVOKESTATIC, "dyvil/reflect/types/PrimitiveType", "apply", "(I)Ldyvil/reflect/types/PrimitiveType;", false);
	}
	
	@Override
	public void writeDefaultValue(MethodWriter writer)
	{
		switch (this.typecode)
		{
		case BOOLEAN_CODE:
		case BYTE_CODE:
		case SHORT_CODE:
		case CHAR_CODE:
		case INT_CODE:
			writer.writeLDC(0);
			break;
		case LONG_CODE:
			writer.writeLDC(0L);
			break;
		case FLOAT_CODE:
			writer.writeLDC(0F);
			break;
		case DOUBLE_CODE:
			writer.writeLDC(0D);
			break;
		}
	}
	
	@Override
	public void writeCast(MethodWriter writer, IType target, int lineNumber) throws BytecodeException
	{
		if (!target.isPrimitive())
		{
			this.boxMethod.writeInvoke(writer, null, EmptyArguments.INSTANCE, lineNumber);
			return;
		}
		
		switch (this.typecode)
		{
		case BOOLEAN_CODE:
		case BYTE_CODE:
		case SHORT_CODE:
		case CHAR_CODE:
		case INT_CODE:
			writeIntCast(target, writer);
			return;
		case LONG_CODE:
			writeLongCast(target, writer);
			return;
		case FLOAT_CODE:
			writeFloatCast(target, writer);
			return;
		case DOUBLE_CODE:
			writeDoubleCast(target, writer);
			return;
		}
	}
	
	private static void writeIntCast(IType cast, MethodWriter writer) throws BytecodeException
	{
		switch (cast.getTypecode())
		{
		case BOOLEAN_CODE:
		case BYTE_CODE:
		case SHORT_CODE:
		case CHAR_CODE:
		case INT_CODE:
			break;
		case LONG_CODE:
			writer.writeInsn(I2L);
			break;
		case FLOAT_CODE:
			writer.writeInsn(I2F);
			break;
		case DOUBLE_CODE:
			writer.writeInsn(I2D);
			break;
		}
	}
	
	private static void writeLongCast(IType cast, MethodWriter writer) throws BytecodeException
	{
		switch (cast.getTypecode())
		{
		case BOOLEAN_CODE:
			writer.writeInsn(L2I);
			break;
		case BYTE_CODE:
			writer.writeInsn(L2B);
			break;
		case SHORT_CODE:
			writer.writeInsn(L2S);
			break;
		case CHAR_CODE:
			writer.writeInsn(L2C);
			break;
		case INT_CODE:
			writer.writeInsn(L2I);
			break;
		case LONG_CODE:
			break;
		case FLOAT_CODE:
			writer.writeInsn(L2F);
			break;
		case DOUBLE_CODE:
			writer.writeInsn(L2D);
			break;
		}
	}
	
	private static void writeFloatCast(IType cast, MethodWriter writer) throws BytecodeException
	{
		switch (cast.getTypecode())
		{
		case BOOLEAN_CODE:
			writer.writeInsn(F2I);
			break;
		case BYTE_CODE:
			writer.writeInsn(F2B);
			break;
		case SHORT_CODE:
			writer.writeInsn(F2S);
			break;
		case CHAR_CODE:
			writer.writeInsn(F2C);
			break;
		case INT_CODE:
			writer.writeInsn(F2I);
			break;
		case LONG_CODE:
			writer.writeInsn(F2L);
			break;
		case FLOAT_CODE:
			break;
		case DOUBLE_CODE:
			writer.writeInsn(F2D);
			break;
		}
	}
	
	private static void writeDoubleCast(IType cast, MethodWriter writer) throws BytecodeException
	{
		switch (cast.getTypecode())
		{
		case BOOLEAN_CODE:
			writer.writeInsn(D2I);
			break;
		case BYTE_CODE:
			writer.writeInsn(D2B);
			break;
		case SHORT_CODE:
			writer.writeInsn(D2S);
			break;
		case CHAR_CODE:
			writer.writeInsn(D2C);
			break;
		case INT_CODE:
			writer.writeInsn(D2I);
			break;
		case LONG_CODE:
			writer.writeInsn(D2L);
			break;
		case FLOAT_CODE:
			writer.writeInsn(D2F);
			break;
		case DOUBLE_CODE:
			break;
		}
	}
	
	@Override
	public IConstantValue getDefaultValue()
	{
		switch (this.typecode)
		{
		case BOOLEAN_CODE:
			return BooleanValue.TRUE;
		case BYTE_CODE:
		case SHORT_CODE:
		case CHAR_CODE:
		case INT_CODE:
			return IntValue.getNull();
		case LONG_CODE:
			return LongValue.getNull();
		case FLOAT_CODE:
			return FloatValue.getNull();
		case DOUBLE_CODE:
			return DoubleValue.getNull();
		}
		return null;
	}
	
	@Override
	public void addAnnotation(IAnnotation annotation, TypePath typePath, int step, int steps)
	{
	}
	
	@Override
	public void writeAnnotations(TypeAnnotatableVisitor visitor, int typeRef, String typePath)
	{
	}
	
	@Override
	public void write(DataOutput out) throws IOException
	{
		out.writeByte(this.typecode);
	}
	
	@Override
	public void read(DataInput in) throws IOException
	{
	}
	
	@Override
	public String toString()
	{
		return this.name.qualified;
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append(this.name);
	}
	
	@Override
	public PrimitiveType clone()
	{
		return this; // no clones
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return this == obj;
	}
	
	@Override
	public int hashCode()
	{
		return this.typecode;
	}
}
