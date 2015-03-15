package dyvil.tools.compiler.ast.type;

import dyvil.tools.compiler.ast.classes.CaptureClass;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeVariable;
import dyvil.tools.compiler.ast.generic.WildcardType;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.lexer.marker.Marker;
import dyvil.tools.compiler.lexer.marker.MarkerList;
import dyvil.tools.compiler.lexer.position.ICodePosition;
import dyvil.tools.compiler.util.Util;

public final class GenericType extends Type implements ITypeList
{
	public IType[]	generics	= new IType[2];
	public int		genericCount;
	
	public GenericType()
	{
		super();
	}
	
	public GenericType(String name)
	{
		super(name);
	}
	
	public GenericType(ICodePosition position, String name)
	{
		super(position, name);
	}
	
	public GenericType(IClass iclass)
	{
		super(iclass);
	}
	
	// ITypeList Overrides
	
	@Override
	public int typeCount()
	{
		return 0;
	}
	
	@Override
	public void setType(int index, IType type)
	{
		this.generics[index] = type;
	}
	
	@Override
	public void addType(IType type)
	{
		int index = this.genericCount++;
		if (this.genericCount > this.generics.length)
		{
			IType[] temp = new IType[this.genericCount];
			System.arraycopy(this.generics, 0, temp, 0, index);
			this.generics = temp;
		}
		this.generics[index] = type;
	}
	
	@Override
	public IType getType(int index)
	{
		return this.generics[index];
	}
	
	// IType Overrides
	
	@Override
	public boolean isGeneric()
	{
		return this.theClass == null || this.theClass.isGeneric();
	}
	
	@Override
	public IType resolveType(String name)
	{
		int len = Math.min(this.theClass.genericCount(), this.genericCount);
		for (int i = 0; i < len; i++)
		{
			if (this.theClass.getTypeVariable(i).isName(name))
			{
				return this.generics[i];
			}
		}
		return null;
	}
	
	@Override
	public IType resolveType(String name, IType concrete)
	{
		if (!concrete.isGeneric())
		{
			return null;
		}
		
		IType type;
		if (this.isSuperTypeOf(concrete))
		{
			IType[] generics = ((GenericType) concrete).generics;
			for (int i = 0; i < this.genericCount; i++)
			{
				type = this.generics[i].resolveType(name, generics[i]);
				if (type != null)
				{
					return type;
				}
			}
		}
		return null;
	}
	
	@Override
	public IType getConcreteType(ITypeContext context)
	{
		GenericType copy = this.clone();
		for (int i = 0; i < this.genericCount; i++)
		{
			copy.generics[i] = this.generics[i].getConcreteType(context);
		}
		return copy;
	}
	
	@Override
	public IType resolve(MarkerList markers, IContext context)
	{
		if (this.theClass != null)
		{
			return this;
		}
		
		IClass iclass;
		if (this.fullName != null)
		{
			iclass = Package.rootPackage.resolveClass(this.fullName);
		}
		else
		{
			iclass = context.resolveClass(this.qualifiedName);
		}
		
		if (iclass != null)
		{
			this.theClass = iclass;
			this.fullName = iclass.getFullName();
			
			if (iclass instanceof CaptureClass)
			{
				return new WildcardType(this.position, this.arrayDimensions, (CaptureClass) iclass);
			}
			
			if (this.generics == null)
			{
				return this;
			}
			
			int varCount = this.theClass.genericCount();
			if (varCount == 0)
			{
				if (this.genericCount != 0 && markers != null)
				{
					markers.add(this.position, "generic.not_generic", this.qualifiedName);
				}
				return this;
			}
			if (varCount != this.genericCount && markers != null)
			{
				markers.add(this.position, "generic.count");
				return this;
			}
			
			for (int i = 0; i < this.genericCount; i++)
			{
				IType t1 = this.generics[i];
				IType t2 = t1.resolve(markers, context);
				if (t1 != t2)
				{
					this.generics[i] = t2;
				}
				
				if (markers != null)
				{
					ITypeVariable var = this.theClass.getTypeVariable(i);
					if (!var.isSuperTypeOf(t2))
					{
						Marker marker = markers.create(t2.getPosition(), "generic.type", var.getQualifiedName());
						marker.addInfo("Generic Type: " + t2);
						marker.addInfo("Type Variable: " + var);
						
					}
				}
			}
			return this;
		}
		if (markers != null)
		{
			markers.add(this.position, "resolve.type", this.toString());
		}
		return this;
	}
	
	@Override
	public String getSignature()
	{
		if (this.generics == null)
		{
			return null;
		}
		
		StringBuilder buf = new StringBuilder();
		this.appendSignature(buf);
		return buf.toString();
	}
	
	@Override
	public void appendSignature(StringBuilder buf)
	{
		for (int i = 0; i < this.arrayDimensions; i++)
		{
			buf.append('[');
		}
		buf.append('L').append(this.getInternalName());
		if (this.generics != null)
		{
			buf.append('<');
			for (int i = 0; i < this.genericCount; i++)
			{
				this.generics[i].appendSignature(buf);
			}
			buf.append('>');
		}
		buf.append(';');
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		for (int i = 0; i < this.arrayDimensions; i++)
		{
			buffer.append('[');
		}
		buffer.append(this.name);
		if (this.generics != null)
		{
			buffer.append('[');
			Util.astToString(prefix, this.generics, this.genericCount, Formatting.Type.genericSeperator, buffer);
			buffer.append(']');
		}
		for (int i = 0; i < this.arrayDimensions; i++)
		{
			buffer.append(']');
		}
	}
	
	@Override
	public GenericType clone()
	{
		GenericType t = new GenericType();
		t.theClass = this.theClass;
		t.name = this.name;
		t.qualifiedName = this.qualifiedName;
		t.fullName = this.fullName;
		t.arrayDimensions = this.arrayDimensions;
		if (this.generics != null)
		{
			t.generics = new IType[this.genericCount];
			System.arraycopy(this.generics, 0, t.generics, 0, this.genericCount);
		}
		return t;
	}
}
