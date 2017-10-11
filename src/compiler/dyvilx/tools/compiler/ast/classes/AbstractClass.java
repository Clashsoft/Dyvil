package dyvilx.tools.compiler.ast.classes;

import dyvil.annotation.internal.NonNull;
import dyvil.annotation.internal.Nullable;
import dyvil.collection.Collection;
import dyvil.collection.List;
import dyvil.collection.Set;
import dyvil.collection.mutable.ArrayList;
import dyvil.reflect.Modifiers;
import dyvil.source.position.SourcePosition;
import dyvilx.tools.compiler.ast.attribute.AttributeList;
import dyvilx.tools.compiler.ast.annotation.IAnnotation;
import dyvilx.tools.compiler.ast.classes.metadata.IClassMetadata;
import dyvilx.tools.compiler.ast.constructor.IConstructor;
import dyvilx.tools.compiler.ast.context.IDefaultContext;
import dyvilx.tools.compiler.ast.expression.IValue;
import dyvilx.tools.compiler.ast.expression.access.FieldAccess;
import dyvilx.tools.compiler.ast.external.ExternalClass;
import dyvilx.tools.compiler.ast.field.*;
import dyvilx.tools.compiler.ast.generic.ITypeContext;
import dyvilx.tools.compiler.ast.generic.ITypeParameter;
import dyvilx.tools.compiler.ast.generic.TypeParameterList;
import dyvilx.tools.compiler.ast.header.IClassCompilable;
import dyvilx.tools.compiler.ast.header.IHeaderUnit;
import dyvilx.tools.compiler.ast.member.IClassMember;
import dyvilx.tools.compiler.ast.method.IMethod;
import dyvilx.tools.compiler.ast.method.MatchList;
import dyvilx.tools.compiler.ast.modifiers.ModifierSet;
import dyvilx.tools.compiler.ast.modifiers.ModifierUtil;
import dyvilx.tools.compiler.ast.parameter.ArgumentList;
import dyvilx.tools.compiler.ast.parameter.ClassParameter;
import dyvilx.tools.compiler.ast.parameter.IParameter;
import dyvilx.tools.compiler.ast.parameter.ParameterList;
import dyvilx.tools.compiler.ast.structure.Package;
import dyvilx.tools.compiler.ast.type.IType;
import dyvilx.tools.compiler.ast.type.TypeList;
import dyvilx.tools.compiler.ast.type.builtin.Types;
import dyvilx.tools.compiler.ast.type.generic.ClassGenericType;
import dyvilx.tools.compiler.ast.type.raw.ClassType;
import dyvilx.tools.compiler.ast.type.typevar.TypeVarType;
import dyvilx.tools.compiler.backend.ClassWriter;
import dyvilx.tools.compiler.config.Formatting;
import dyvilx.tools.compiler.sources.DyvilFileType;
import dyvilx.tools.compiler.transform.Deprecation;
import dyvilx.tools.compiler.util.Util;
import dyvil.lang.Name;
import dyvilx.tools.parsing.marker.MarkerList;

import java.lang.annotation.ElementType;

public abstract class AbstractClass implements IClass, IDefaultContext
{
	// Modifiers and Annotations

	protected @Nullable AttributeList annotations;
	protected @NonNull  ModifierSet   modifiers;

	// Signature

	protected Name name;

	protected @Nullable TypeParameterList typeParameters;

	protected @NonNull ParameterList parameters = new ParameterList();

	protected IType superType = Types.OBJECT;
	protected @Nullable TypeList interfaces;

	// Body

	protected ClassBody body;

	// Metadata

	protected String fullName;
	protected String internalName;

	protected IClass         enclosingClass;
	protected IClassMetadata metadata;

	protected IClassCompilable[] compilables;
	protected int                compilableCount;

	protected IType thisType;

	protected IType classType = new ClassType(this);

	@Override
	public abstract IHeaderUnit getHeader();

	@Override
	public IClass getEnclosingClass()
	{
		return this.enclosingClass;
	}

	@Override
	public void setEnclosingClass(IClass enclosingClass)
	{
		this.enclosingClass = enclosingClass;
	}

	@Override
	public IType getClassType()
	{
		return this.classType;
	}

	@Override
	public ElementType getElementType()
	{
		if (this.isAnnotation())
		{
			return ElementType.ANNOTATION_TYPE;
		}
		return ElementType.TYPE;
	}

	@Override
	public AttributeList getAttributes()
	{
		if (this.annotations != null)
		{
			return this.annotations;
		}
		return this.annotations = new AttributeList();
	}

	@Override
	public void setAttributes(AttributeList attributes)
	{
		this.annotations = attributes;
	}

	@Override
	public final IAnnotation getAnnotation(IClass type)
	{
		return this.annotations == null ? null : this.getAttributes().get(type);
	}

	@Override
	public boolean addRawAnnotation(String type, IAnnotation annotation)
	{
		switch (type)
		{
		case ModifierUtil.STRICTFP_INTERNAL:
			this.modifiers.addIntModifier(Modifiers.STRICT);
			return false;
		case Deprecation.DYVIL_INTERNAL:
		case Deprecation.JAVA_INTERNAL:
			this.modifiers.addIntModifier(Modifiers.DEPRECATED);
			return true;
		case "java/lang/FunctionalInterface":
			this.modifiers.addIntModifier(Modifiers.FUNCTIONAL);
			return false;
		}
		return true;
	}

	@Override
	public ModifierSet getModifiers()
	{
		return this.modifiers;
	}

	@Override
	public void setModifiers(ModifierSet modifiers)
	{
		this.modifiers = modifiers;
	}

	@Override
	public boolean hasModifier(int mod)
	{
		return this.modifiers.hasIntModifier(mod);
	}

	@Override
	public int getAccessLevel()
	{
		return this.modifiers.toFlags() & Modifiers.ACCESS_MODIFIERS;
	}

	// Names

	@Override
	public void setName(Name name)
	{
		this.name = name;
	}

	@Override
	public Name getName()
	{
		return this.name;
	}

	@Override
	public void setFullName(String name)
	{
		this.fullName = name;
	}

	@Override
	public String getFullName()
	{
		return this.fullName;
	}

	// Generics

	@Override
	public boolean isTypeParametric()
	{
		return this.typeParameters != null && this.typeParameters.size() > 0;
	}

	@Override
	public TypeParameterList getTypeParameters()
	{
		if (this.typeParameters != null)
		{
			return this.typeParameters;
		}
		return this.typeParameters = new TypeParameterList();
	}

	// Class Parameters

	@Override
	public ParameterList getParameters()
	{
		return this.parameters;
	}

	@Override
	public IParameter createParameter(SourcePosition position, Name name, IType type, ModifierSet modifiers,
		                                 AttributeList annotations)
	{
		return new ClassParameter(this, position, name, type, modifiers, annotations);
	}

	// Super Types

	@Override
	public void setSuperType(IType type)
	{
		this.superType = type;
	}

	@Override
	public IType getSuperType()
	{
		return this.superType;
	}

	@Override
	public boolean isSubClassOf(IType type)
	{
		IClass iclass = type.getTheClass();
		if (this == iclass || this.superType != null && Types.isSuperClass(type, this.superType))
		{
			return true;
		}
		if (!iclass.isInterface() || this.interfaces == null)
		{
			return false;
		}
		for (IType interfaceType : this.interfaces)
		{
			if (Types.isSuperClass(type, interfaceType))
			{
				return true;
			}
		}
		return false;
	}

	// Interfaces

	@Override
	public TypeList getInterfaces()
	{
		if (this.interfaces != null)
		{
			return this.interfaces;
		}
		return this.interfaces = new TypeList();
	}

	// Body

	@Override
	public void setBody(ClassBody body)
	{
		this.body = body;
	}

	@Override
	public ClassBody getBody()
	{
		return this.body;
	}

	@Override
	public int classCompilableCount()
	{
		return this.compilableCount;
	}

	@Override
	public void addClassCompilable(IClassCompilable compilable)
	{
		if (this.compilables == null)
		{
			this.compilables = new IClassCompilable[2];
			this.compilables[0] = compilable;
			this.compilableCount = 1;
			return;
		}

		int index = this.compilableCount++;
		if (this.compilableCount > this.compilables.length)
		{
			IClassCompilable[] temp = new IClassCompilable[this.compilableCount];
			System.arraycopy(this.compilables, 0, temp, 0, index);
			this.compilables = temp;
		}
		this.compilables[index] = compilable;
	}

	@Override
	public void setMetadata(IClassMetadata metadata)
	{
		this.metadata = metadata;
	}

	@Override
	public IClassMetadata getMetadata()
	{
		return this.metadata;
	}

	@Override
	public Collection<IMethod> getMethods(Name name)
	{
		final List<IMethod> list = new ArrayList<>();

		for (int i = 0, count = this.parameters.size(); i < count; i++)
		{
			final IProperty property = this.parameters.get(i).getProperty();
			if (property != null)
			{
				final IMethod getter = property.getGetter();
				if (getter != null && name == getter.getName())
				{
					list.add(getter);
				}

				final IMethod setter = property.getSetter();
				if (setter != null && name == setter.getName())
				{
					list.add(setter);
				}
			}
		}

		if (this.body != null)
		{
			for (int i = 0, count = this.body.methodCount(); i < count; i++)
			{
				final IMethod method = this.body.getMethod(i);
				if (method != null)
				{
					list.add(method);
				}
			}
		}

		return list;
	}

	@Override
	public boolean checkImplements(IMethod candidate, ITypeContext typeContext)
	{
		if (candidate.getEnclosingClass() == this)
		{
			return !candidate.hasModifier(Modifiers.ABSTRACT);
		}

		for (int i = 0, count = this.parameters.size(); i < count; i++)
		{
			final IProperty property = this.parameters.get(i).getProperty();
			if (property != null && ClassBody.checkPropertyImplements(property, candidate, typeContext))
			{
				return true;
			}
		}

		if (this.metadata.checkImplements(candidate, typeContext))
		{
			return true;
		}

		if (this.body != null && this.body.checkImplements(candidate, typeContext))
		{
			return true;
		}

		if (this.superType != null)
		{
			final IClass superClass = this.superType.getTheClass();
			if (superClass != null && superClass
				                          .checkImplements(candidate, this.superType.getConcreteType(typeContext)))
			{
				return true;
			}
		}

		if (this.interfaces != null)
		{
			for (IType interfaceType : this.interfaces)
			{
				final IClass interfaceClass = interfaceType.getTheClass();
				if (interfaceClass != null && interfaceClass.checkImplements(candidate, interfaceType.getConcreteType(
					typeContext)))
				{
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void checkMethods(MarkerList markers, IClass checkedClass, ITypeContext typeContext,
		                        Set<IClass> checkedClasses)
	{
		if (checkedClasses.contains(this))
		{
			return;
		}
		checkedClasses.add(this);

		for (int i = 0, count = this.parameters.size(); i < count; i++)
		{
			final IProperty property = this.parameters.get(i).getProperty();
			if (property != null)
			{
				ClassBody.checkProperty(property, markers, checkedClass, typeContext);
			}
		}

		if (this.body != null)
		{
			this.body.checkMethods(markers, checkedClass, typeContext);
		}

		this.checkSuperMethods(markers, checkedClass, typeContext, checkedClasses);
	}

	public void checkSuperMethods(MarkerList markers, IClass thisClass, ITypeContext typeContext,
		                             Set<IClass> checkedClasses)
	{
		if (this.superType != null)
		{
			final IClass superClass = this.superType.getTheClass();
			if (superClass != null && superClass != this)
			{
				superClass
					.checkMethods(markers, thisClass, this.superType.getConcreteType(typeContext), checkedClasses);
			}
		}
		if (this.interfaces != null)
		{
			for (IType type : this.interfaces)
			{
				final IClass iClass = type.getTheClass();
				if (iClass != null && iClass != this)
				{
					iClass.checkMethods(markers, thisClass, type.getConcreteType(typeContext), checkedClasses);
				}
			}
		}
	}

	@Override
	public IMethod getFunctionalMethod()
	{
		return this.metadata.getFunctionalMethod();
	}

	@Override
	public String getInternalName()
	{
		return this.internalName;
	}

	@Override
	public String getSignature()
	{
		StringBuilder buffer = new StringBuilder();

		if (this.typeParameters != null)
		{
			this.typeParameters.appendSignature(buffer);
		}

		if (this.superType != null)
		{
			this.superType.appendSignature(buffer, false);
		}
		if (this.interfaces != null)
		{
			this.interfaces.appendDescriptors(buffer, IType.NAME_SIGNATURE);
		}
		return buffer.toString();
	}

	@Override
	public String[] getInterfaceArray()
	{
		if (this.interfaces == null)
		{
			return null;
		}

		final int size = this.interfaces.size();
		final String[] interfaces = new String[size];
		for (int i = 0; i < size; i++)
		{
			interfaces[i] = this.interfaces.get(i).getInternalName();
		}
		return interfaces;
	}

	@Override
	public IType resolveType(ITypeParameter typeParameter, IType concrete)
	{
		if (this.superType != null)
		{
			final IType type = this.superType.resolveType(typeParameter);
			if (type != null)
			{
				return type.getConcreteType(concrete);
			}
		}
		if (this.interfaces != null)
		{
			for (IType interfaceType : this.interfaces)
			{
				final IType type = interfaceType.resolveType(typeParameter);
				if (type != null)
				{
					return type.getConcreteType(concrete);
				}
			}
		}
		return null;
	}

	@Override
	public byte checkStatic()
	{
		return FALSE;
	}

	@Override
	public IClass getThisClass()
	{
		return this;
	}

	@Override
	public IType getThisType()
	{
		if (this.thisType != null)
		{
			return this.thisType;
		}

		if (this.typeParameters == null)
		{
			return this.thisType = this.classType;
		}

		final ClassGenericType type = new ClassGenericType(this);
		final TypeList arguments = type.getArguments();
		for (int i = 0; i < this.typeParameters.size(); i++)
		{
			arguments.add(new TypeVarType(this.typeParameters.get(i)));
		}
		return this.thisType = type;
	}

	@Override
	public Package resolvePackage(Name name)
	{
		return null;
	}

	@Override
	public IClass resolveClass(Name name)
	{
		return this.resolveInnerClass(name);
	}

	private IClass resolveInnerClass(Name name)
	{
		if (this.body != null)
		{
			return this.body.getClass(name);
		}
		return null;
	}

	@Override
	public ITypeParameter resolveTypeParameter(Name name)
	{
		return this.typeParameters == null ? null : this.typeParameters.get(name);
	}

	protected IParameter resolveClassParameter(Name name)
	{
		final IParameter parameter = this.parameters.get(name);
		// do not expose override class parameters
		return parameter == null || parameter.hasModifier(Modifiers.OVERRIDE) ? null : parameter;
	}

	@Override
	public IDataMember resolveField(Name name)
	{
		final IParameter parameter = this.resolveClassParameter(name);
		if (parameter != null)
		{
			return parameter;
		}

		IDataMember field;
		if (this.body != null)
		{
			// Own fields
			field = this.body.getField(name);
			if (field != null)
			{
				return field;
			}
		}

		field = this.metadata.resolveField(name);
		if (field != null)
		{
			return field;
		}

		// Inherited Fields
		if (this.superType != null)
		{
			field = this.superType.resolveField(name);
			if (field != null)
			{
				return field;
			}
		}

		return null;
	}

	@Override
	public IValue resolveImplicit(IType type)
	{
		if (type == null)
		{
			return null;
		}

		IParameter candidate = null;
		for (IParameter param : this.parameters)
		{
			if (!param.isImplicit() || !Types.isSuperType(param.getType(), type))
			{
				continue;
			}
			if (candidate != null)
			{
				return null; // ambiguous -> pick none
			}
			candidate = param;
		}
		if (candidate != null)
		{
			return new FieldAccess(candidate);
		}

		return this.body == null ? null : this.body.resolveImplicit(type);
	}

	@Override
	public void getMethodMatches(MatchList<IMethod> list, IValue receiver, Name name, ArgumentList arguments)
	{
		for (int i = 0, count = this.parameters.size(); i < count; i++)
		{
			final IProperty property = this.parameters.get(i).getProperty();
			if (property != null)
			{
				property.checkMatch(list, receiver, name, arguments);
			}
		}

		if (this.body != null)
		{
			this.body.getMethodMatches(list, receiver, name, arguments);
		}

		this.metadata.getMethodMatches(list, receiver, name, arguments);

		if (list.hasCandidate())
		{
			return;
		}

		if (this.superType != null)
		{
			this.superType.getMethodMatches(list, receiver, name, arguments);
		}

		if (list.hasCandidate() || this.interfaces == null)
		{
			return;
		}

		for (IType type : this.interfaces)
		{
			type.getMethodMatches(list, receiver, name, arguments);
		}
	}

	@Override
	public void getImplicitMatches(MatchList<IMethod> list, IValue value, IType targetType)
	{
		if (this.body != null)
		{
			this.body.getImplicitMatches(list, value, targetType);
		}

		// TODO look into super types?
	}

	@Override
	public void getConstructorMatches(MatchList<IConstructor> list, ArgumentList arguments)
	{
		if (this.body != null)
		{
			this.body.getConstructorMatches(list, arguments);
		}

		this.metadata.getConstructorMatches(list, arguments);
	}

	@Override
	public IType getReturnType()
	{
		return null;
	}

	@Override
	public byte checkException(IType type)
	{
		return FALSE;
	}

	@Override
	public boolean isMember(IVariable variable)
	{
		return this.parameters.isParameter(variable);
	}

	@Override
	public IDataMember capture(IVariable variable)
	{
		return variable;
	}

	@Override
	public IAccessible getAccessibleThis(IClass type)
	{
		return type == this || Types.isSuperClass(type, this) ? VariableThis.DEFAULT : null;
	}

	@Override
	public boolean isMember(IClassMember member)
	{
		return member.getEnclosingClass() == this;
	}

	@Override
	public byte getVisibility(IClassMember member)
	{
		final IClass enclosingClass = member.getEnclosingClass();
		if (enclosingClass == this)
		{
			return VISIBLE;
		}

		int level = member.getAccessLevel();
		if ((level & Modifiers.INTERNAL) != 0)
		{
			if (enclosingClass instanceof ExternalClass)
			{
				return INTERNAL;
			}
			level &= ~Modifiers.INTERNAL;
		}

		if (level == Modifiers.PUBLIC)
		{
			return VISIBLE;
		}
		if (level == Modifiers.PROTECTED || level == Modifiers.PRIVATE_PROTECTED)
		{
			if (Types.isSuperClass(enclosingClass, this))
			{
				// The enclosing class of the member is a super class of this
				return VISIBLE;
			}
		}
		if (level == Modifiers.PROTECTED || level == Modifiers.PACKAGE)
		{
			final IHeaderUnit thisUnit = this.getHeader();
			final IHeaderUnit memberUnit = enclosingClass.getHeader();
			if (thisUnit != null && memberUnit != null && thisUnit.getPackage() == memberUnit.getPackage())
			{
				// The two units are in the same package
				return VISIBLE;
			}
		}

		return INVISIBLE;
	}

	@Override
	public String getFileName()
	{
		final String internalName = this.getInternalName();
		final int slashIndex = internalName.lastIndexOf('/');

		if (slashIndex < 0)
		{
			return internalName + DyvilFileType.CLASS_EXTENSION;
		}
		// Strip the package/ part
		return internalName.substring(slashIndex + 1) + DyvilFileType.CLASS_EXTENSION;
	}

	@Override
	public void writeInnerClassInfo(ClassWriter writer)
	{
		if (this.enclosingClass != null)
		{
			final int modifiers = this.modifiers.toFlags() & ModifierUtil.JAVA_MODIFIER_MASK;
			final String outerName = this.enclosingClass.getInternalName();
			writer.visitInnerClass(this.getInternalName(), outerName, this.name.qualified, modifiers);
		}
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		this.toString("", builder);
		return builder.toString();
	}

	@Override
	public void toString(@NonNull String indent, @NonNull StringBuilder buffer)
	{
		if (this.annotations != null)
		{
			this.annotations.toString(indent, buffer);
		}
		this.modifiers.toString(this.getKind(), buffer);

		ModifierUtil.writeClassType(this.modifiers.toFlags(), buffer);
		buffer.append(this.name);

		if (this.typeParameters != null)
		{
			this.typeParameters.toString(indent, buffer);
		}

		if (!this.parameters.isEmpty())
		{
			this.parameters.toString(indent, buffer);
		}

		if (this.superType != null && this.superType.getTheClass() != Types.OBJECT_CLASS)
		{
			String extendsPrefix = indent;
			if (Formatting.getBoolean("class.extends.newline"))
			{
				extendsPrefix = Formatting.getIndent("class.extends.indent", extendsPrefix);
				buffer.append('\n').append(extendsPrefix).append("extends ");
			}
			else
			{
				buffer.append(" extends ");
			}

			this.superType.toString("", buffer);
		}

		if (this.interfaces != null && this.interfaces.size() > 0)
		{
			final String itfIndent;
			if (Formatting.getBoolean("class.implements.newline"))
			{
				itfIndent = Formatting.getIndent("class.implements.indent", indent);
				buffer.append('\n').append(itfIndent);
			}
			else
			{
				itfIndent = indent;
				buffer.append(' ');
			}

			buffer.append(this.isInterface() ? "extends " : "implements ");

			Util.astToString(itfIndent, this.interfaces.getTypes(), this.interfaces.size(),
			                 Formatting.getSeparator("class.implements.separator", ','), buffer);
		}

		if (this.body != null)
		{
			this.body.toString(indent, buffer);
		}
		else if (Formatting.getBoolean("class.semicolon"))
		{
			buffer.append(';');
		}
	}
}
