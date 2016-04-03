package dyvil.tools.compiler.ast.type.typevar;

import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeParameter;
import dyvil.tools.compiler.ast.type.IType;

public class CovariantTypeVarType extends TypeVarType
{
	public CovariantTypeVarType(ITypeParameter typeVariable)
	{
		super(typeVariable);
	}

	@Override
	public IType asParameterType()
	{
		return this;
	}

	@Override
	public boolean isSameType(IType type)
	{
		return type.getTypeVariable() != null && this.typeParameter.isSuperTypeOf(type);
	}

	@Override
	public boolean isSuperClassOf(IType subType)
	{
		return this.typeParameter.isSuperClassOf(subType);
	}

	@Override
	public boolean isSuperTypeOf(IType type)
	{
		return this.typeParameter.isSuperTypeOf(type);
	}

	@Override
	public IType getConcreteType(ITypeContext context)
	{
		IType type = super.getConcreteType(context);
		if (type.getTypeVariable() == this.typeParameter)
		{
			return this;
		}
		return type;
	}
}
