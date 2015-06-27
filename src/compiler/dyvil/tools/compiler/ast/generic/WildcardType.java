package dyvil.tools.compiler.ast.generic;

import dyvil.lang.List;

import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.member.IClassMember;
import dyvil.tools.compiler.ast.member.Name;
import dyvil.tools.compiler.ast.method.ConstructorMatch;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MethodMatch;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.lexer.marker.MarkerList;
import dyvil.tools.compiler.lexer.position.ICodePosition;

public final class WildcardType extends BaseBounded implements IType
{
	public WildcardType()
	{
	}
	
	public WildcardType(ICodePosition position)
	{
		this.position = position;
	}
	
	@Override
	public int typeTag()
	{
		return WILDCARD_TYPE;
	}
	
	@Override
	public void setName(Name name)
	{
	}
	
	@Override
	public Name getName()
	{
		return null;
	}
	
	@Override
	public void setClass(IClass theClass)
	{
	}
	
	@Override
	public IClass getTheClass()
	{
		return null;
	}
	
	@Override
	public IType getSuperType()
	{
		return this.upperBoundCount == 0 ? Types.UNKNOWN : this.upperBounds[0];
	}
	
	@Override
	public boolean equals(IType type)
	{
		if (this.upperBoundCount > 0)
		{
			for (int i = 0; i < this.upperBoundCount; i++)
			{
				if (!this.upperBounds[i].isSuperTypeOf(type))
				{
					return false;
				}
			}
		}
		if (this.lowerBound != null)
		{
			if (!type.isSuperTypeOf(this.lowerBound))
			{
				return false;
			}
		}
		return !type.isPrimitive();
	}
	
	@Override
	public boolean isSuperTypeOf(IType type)
	{
		return super.isSuperTypeOf(type);
	}
	
	@Override
	public boolean isResolved()
	{
		return true;
	}
	
	@Override
	public IType resolve(MarkerList markers, IContext context)
	{
		this.resolveTypes(markers, context);
		return this;
	}
	
	@Override
	public IType resolveType(ITypeVariable typeVar)
	{
		IType type;
		for (int i = 0; i < this.upperBoundCount; i++)
		{
			type = this.upperBounds[i].resolveType(typeVar);
			if (type != null)
			{
				return type;
			}
		}
		return null;
	}
	
	@Override
	public IType resolveType(ITypeVariable typeVar, IType concrete)
	{
		IType type;
		for (int i = 0; i < this.upperBoundCount; i++)
		{
			type = this.upperBounds[i].resolveType(typeVar, concrete);
			if (type != null)
			{
				return type;
			}
		}
		return null;
	}
	
	@Override
	public boolean hasTypeVariables()
	{
		return false;
	}
	
	@Override
	public IType getConcreteType(ITypeContext context)
	{
		if (this.lowerBound != null)
		{
			return this.lowerBound.getConcreteType(context);
		}
		
		WildcardType type = new WildcardType(this.position);
		type.upperBounds = new IType[this.upperBoundCount];
		type.upperBoundCount = this.upperBoundCount;
		for (int i = 0; i < this.upperBoundCount; i++)
		{
			type.upperBounds[i] = this.upperBounds[i].getConcreteType(context);
		}
		return type;
	}
	
	@Override
	public boolean isStatic()
	{
		return true;
	}
	
	@Override
	public IClass getThisClass()
	{
		return null;
	}
	
	@Override
	public Package resolvePackage(Name name)
	{
		return null;
	}
	
	@Override
	public IClass resolveClass(Name name)
	{
		return null;
	}
	
	@Override
	public ITypeVariable resolveTypeVariable(Name name)
	{
		for (int i = 0; i < this.upperBoundCount; i++)
		{
			ITypeVariable var = this.upperBounds[i].resolveTypeVariable(name);
			if (var != null)
			{
				return var;
			}
		}
		
		return null;
	}
	
	@Override
	public IDataMember resolveField(Name name)
	{
		for (int i = 0; i < this.upperBoundCount; i++)
		{
			IDataMember f = this.upperBounds[i].resolveField(name);
			if (f != null)
			{
				return f;
			}
		}
		
		return null;
	}
	
	@Override
	public void getMethodMatches(List<MethodMatch> list, IValue instance, Name name, IArguments arguments)
	{
		if (this.upperBoundCount == 0)
		{
			Types.OBJECT_CLASS.getMethodMatches(list, instance, name, arguments);
			return;
		}
		
		for (int i = 0; i < this.upperBoundCount; i++)
		{
			this.upperBounds[i].getMethodMatches(list, instance, name, arguments);
		}
	}
	
	@Override
	public void getConstructorMatches(List<ConstructorMatch> list, IArguments arguments)
	{
	}
	
	@Override
	public byte getVisibility(IClassMember member)
	{
		return 0;
	}
	
	@Override
	public IMethod getFunctionalMethod()
	{
		return null;
	}
	
	@Override
	public void setInternalName(String name)
	{
	}
	
	@Override
	public String getInternalName()
	{
		if (this.upperBoundCount > 0)
		{
			return this.upperBounds[0].getInternalName();
		}
		return "java/lang/Object";
	}
	
	@Override
	public void appendExtendedName(StringBuilder buffer)
	{
		buffer.append('L').append(this.getInternalName()).append(';');
	}
	
	@Override
	public void appendSignature(StringBuilder buffer)
	{
		if (this.lowerBound != null)
		{
			buffer.append('-');
			this.lowerBound.appendSignature(buffer);
		}
		else if (this.upperBoundCount > 0)
		{
			buffer.append('+');
			this.upperBounds[0].appendSignature(buffer);
		}
		else
		{
			buffer.append("Ljava/lang/Object;");
		}
	}
	
	@Override
	public int getLoadOpcode()
	{
		return Opcodes.ALOAD;
	}
	
	@Override
	public int getArrayLoadOpcode()
	{
		return Opcodes.AALOAD;
	}
	
	@Override
	public int getStoreOpcode()
	{
		return Opcodes.ASTORE;
	}
	
	@Override
	public int getArrayStoreOpcode()
	{
		return Opcodes.AASTORE;
	}
	
	@Override
	public int getReturnOpcode()
	{
		return Opcodes.ARETURN;
	}
	
	@Override
	public void writeTypeExpression(MethodWriter writer) throws BytecodeException
	{
		if (this.lowerBound != null)
		{
			this.lowerBound.writeTypeExpression(writer);
		}
		else
		{
			writer.writeInsn(Opcodes.ACONST_NULL);
		}
		
		writer.writeLDC(this.upperBoundCount);
		writer.writeNewArray("dyvil/lang/Type", 1);
		for (int i = 0; i < this.upperBoundCount; i++)
		{
			writer.writeInsn(Opcodes.DUP);
			writer.writeLDC(i);
			this.upperBounds[i].writeTypeExpression(writer);
			writer.writeInsn(Opcodes.AASTORE);
		}
		
		writer.writeInvokeInsn(Opcodes.INVOKESTATIC, "dyvil/reflect/type/WildcardType", "apply",
				"(Ldyvil/lang/Type;[Ldyvil/lang/Type;)Ldyvil/reflect/type/WildcardType;", false);
	}
	
	@Override
	public WildcardType clone()
	{
		WildcardType clone = new WildcardType(this.position);
		clone.lowerBound = this.lowerBound;
		clone.upperBoundCount = this.upperBoundCount;
		clone.upperBounds = this.upperBounds;
		return clone;
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append('_');
		if (this.lowerBound != null)
		{
			buffer.append(Formatting.Type.genericLowerBound);
			this.lowerBound.toString(prefix, buffer);
		}
		if (this.upperBoundCount > 0)
		{
			buffer.append(Formatting.Type.genericUpperBound);
			this.upperBounds[0].toString(prefix, buffer);
			for (int i = 1; i < this.upperBoundCount; i++)
			{
				buffer.append(Formatting.Type.genericBoundSeperator);
				this.upperBounds[i].toString(prefix, buffer);
			}
		}
	}
}
