package dyvil.tools.compiler.ast.type;

import dyvil.lang.List;

import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.IASTNode;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.constant.IConstantValue;
import dyvil.tools.compiler.ast.constant.NullValue;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeVariable;
import dyvil.tools.compiler.ast.member.IClassMember;
import dyvil.tools.compiler.ast.member.INamed;
import dyvil.tools.compiler.ast.member.Name;
import dyvil.tools.compiler.ast.method.ConstructorMatch;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MethodMatch;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.structure.IDyvilHeader;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.lexer.marker.MarkerList;

public interface IType extends IASTNode, INamed, IContext, ITypeContext
{
	int	UNKNOWN			= -1;
	int	NULL			= 0;
	int	TYPE			= 1;
	int	PRIMITIVE_TYPE	= 2;
	int	GENERIC_TYPE	= 3;
	
	int	TYPE_VAR_TYPE	= 4;
	int	WILDCARD_TYPE	= 5;
	
	int	ARRAY_TYPE		= 6;
	int	MULTI_ARRAY		= 7;
	int	TUPLE_TYPE		= 8;
	int	FUNCTION_TYPE	= 9;
	
	int	DYNAMIC			= 10;
	
	public int typeTag();
	
	public default boolean isPrimitive()
	{
		return false;
	}
	
	public default boolean isGenericType()
	{
		return false;
	}
	
	public default IType getReferenceType()
	{
		return this;
	}
	
	public default IMethod getBoxMethod()
	{
		return null;
	}
	
	public default IMethod getUnboxMethod()
	{
		return null;
	}
	
	// Full Name
	
	@Override
	public void setName(Name name);
	
	@Override
	public Name getName();
	
	// Container Class
	
	public void setClass(IClass theClass);
	
	public IClass getTheClass();
	
	// Arrays
	
	public default boolean isArrayType()
	{
		return false;
	}
	
	public default int getArrayDimensions()
	{
		return 0;
	}
	
	public default IType getElementType()
	{
		return this;
	}
	
	public default IClass getArrayClass()
	{
		return Types.getObjectArray();
	}
	
	// Super Type
	
	public IType getSuperType();
	
	/**
	 * Returns true if {@code type} is a subtype of this type
	 * 
	 * @param type
	 * @return
	 */
	public default boolean isSuperTypeOf(IType type)
	{
		IClass thisClass = this.getTheClass();
		IClass thatClass = type.getTheClass();
		if (type.isArrayType())
		{
			return thisClass == Types.OBJECT_CLASS;
		}
		if (thatClass != null)
		{
			return thatClass == thisClass || thatClass.isSubTypeOf(this);
		}
		return false;
	}
	
	public default boolean isSuperTypeOf2(IType type)
	{
		IClass thisClass = this.getTheClass();
		IClass thatClass = type.getTheClass();
		if (thatClass != null)
		{
			return thatClass == thisClass || thatClass.isSubTypeOf(this);
		}
		return false;
	}
	
	public default boolean equals(IType type)
	{
		return this.getTheClass() == type.getTheClass();
	}
	
	public default boolean classEquals(IType type)
	{
		return this.getTheClass() == type.getTheClass();
	}
	
	// Resolve
	
	public boolean isResolved();
	
	public IType resolve(MarkerList markers, IContext context);
	
	// Generics
	
	/**
	 * Returns true if this is or contains any type variables.
	 * 
	 * @return
	 */
	public boolean hasTypeVariables();
	
	/**
	 * Returns a copy of this type with all type variables replaced.
	 * 
	 * @param typeVariables
	 *            the type variables
	 * @return
	 */
	public IType getConcreteType(ITypeContext context);
	
	@Override
	public default IType resolveType(ITypeVariable typeVar)
	{
		return null;
	}
	
	public default IType resolveType(ITypeVariable typeVar, IType concrete)
	{
		return null;
	}
	
	// IContext
	
	@Override
	public default boolean isStatic()
	{
		return true;
	}
	
	@Override
	public default IDyvilHeader getHeader()
	{
		return this.getTheClass().getHeader();
	}
	
	@Override
	public default IClass getThisClass()
	{
		return this.getTheClass();
	}
	
	@Override
	public Package resolvePackage(Name name);
	
	@Override
	public IClass resolveClass(Name name);
	
	@Override
	public ITypeVariable resolveTypeVariable(Name name);
	
	@Override
	public IDataMember resolveField(Name name);
	
	@Override
	public void getMethodMatches(List<MethodMatch> list, IValue instance, Name name, IArguments arguments);
	
	@Override
	public void getConstructorMatches(List<ConstructorMatch> list, IArguments arguments);
	
	@Override
	public byte getVisibility(IClassMember member);
	
	@Override
	public default boolean handleException(IType type)
	{
		return false;
	}
	
	public IMethod getFunctionalMethod();
	
	// Compilation
	
	public void setInternalName(String name);
	
	public String getInternalName();
	
	public default String getExtendedName()
	{
		StringBuilder buffer = new StringBuilder();
		this.appendExtendedName(buffer);
		return buffer.toString();
	}
	
	public void appendExtendedName(StringBuilder buffer);
	
	public default String getSignature()
	{
		return null;
	}
	
	public void appendSignature(StringBuilder buffer);
	
	// Compilation
	
	public default int getLoadOpcode()
	{
		return Opcodes.ALOAD;
	}
	
	public default int getArrayLoadOpcode()
	{
		return Opcodes.AALOAD;
	}
	
	public default int getStoreOpcode()
	{
		return Opcodes.ASTORE;
	}
	
	public default int getArrayStoreOpcode()
	{
		return Opcodes.AASTORE;
	}
	
	public default int getReturnOpcode()
	{
		return Opcodes.ARETURN;
	}
	
	public default Object getFrameType()
	{
		return this.getInternalName();
	}
	
	public void writeTypeExpression(MethodWriter writer) throws BytecodeException;
	
	public default void writeDefaultValue(MethodWriter writer) throws BytecodeException
	{
		writer.writeInsn(Opcodes.ACONST_NULL);
	}
	
	public default IConstantValue getDefaultValue()
	{
		return NullValue.getNull();
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer);
	
	// Misc
	
	public IType clone();
}
