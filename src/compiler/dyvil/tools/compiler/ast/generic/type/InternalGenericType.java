package dyvil.tools.compiler.ast.generic.type;

import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeVariable;
import dyvil.tools.compiler.ast.method.ConstructorMatchList;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MethodMatchList;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.reference.ReferenceType;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.*;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class InternalGenericType extends GenericType
{
	protected String internalName;
	
	public InternalGenericType(String internal)
	{
		this.internalName = internal;
	}
	
	@Override
	public int typeTag()
	{
		return GENERIC_INTERNAL;
	}
	
	@Override
	public Name getName()
	{
		return Name.getQualified(this.internalName.substring(this.internalName.lastIndexOf('/') + 1));
	}
	
	@Override
	public IClass getTheClass()
	{
		return Types.OBJECT_CLASS;
	}
	
	@Override
	public void inferTypes(IType concrete, ITypeContext typeContext)
	{
	}
	
	@Override
	public IType resolveType(ITypeVariable typeVar)
	{
		return null;
	}
	
	@Override
	public boolean isResolved()
	{
		return false;
	}
	
	@Override
	public IType resolveType(MarkerList markers, IContext context)
	{
		for (int i = 0; i < this.typeArgumentCount; i++)
		{
			this.typeArguments[i] = this.typeArguments[i].resolveType(markers, context);
		}
		
		if (this.internalName.startsWith("dyvil/tuple/Tuple"))
		{
			return new TupleType(this.typeArguments, this.typeArgumentCount);
		}
		if (this.internalName.startsWith("dyvil/function/Function"))
		{
			int i = this.typeArgumentCount - 1;
			IType returnType = this.typeArguments[i];
			this.typeArguments[i] = null;
			return new LambdaType(this.typeArguments, i, returnType);
		}

		switch (this.internalName)
		{
		case "dyvil/lang/ref/ObjectRef":
			return new ReferenceType(Types.getObjectRefClass(), this.typeArguments[0]);
		case "dyvil/collection/ImmutableMap":
			return new MapType(this.typeArguments[0], this.typeArguments[1], Mutability.MUTABLE,
			                   MapType.MapTypes.MUTABLE_MAP_CLASS);
		case "dyvil/collection/MutableMap":
			return new MapType(this.typeArguments[0], this.typeArguments[1], Mutability.IMMUTABLE,
			                   MapType.MapTypes.IMMUTABLE_MAP_CLASS);
		case "dyvil/collection/Map":
			return new MapType(this.typeArguments[0], this.typeArguments[1], Mutability.UNDEFINED,
			                   MapType.MapTypes.MAP_CLASS);
		}

		final IClass iclass = Package.rootPackage.resolveInternalClass(this.internalName);
		return new ClassGenericType(iclass, this.typeArguments, this.typeArgumentCount);
	}
	
	@Override
	public void checkType(MarkerList markers, IContext context, TypePosition position)
	{
	}
	
	@Override
	public IDataMember resolveField(Name name)
	{
		return null;
	}
	
	@Override
	public void getMethodMatches(MethodMatchList list, IValue instance, Name name, IArguments arguments)
	{
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
		return this.internalName;
	}
	
	@Override
	public void write(DataOutput out) throws IOException
	{
		out.writeUTF(this.internalName);
	}
	
	@Override
	public void read(DataInput in) throws IOException
	{
		this.internalName = in.readUTF();
	}
	
	@Override
	public GenericType clone()
	{
		InternalGenericType copy = new InternalGenericType(this.internalName);
		this.copyTypeArguments(copy);
		return copy;
	}
}
