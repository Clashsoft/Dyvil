package dyvil.tools.compiler.ast.parameter;

import dyvil.reflect.Modifiers;
import dyvil.tools.compiler.ast.annotation.AnnotationList;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.expression.ThisExpr;
import dyvil.tools.compiler.ast.field.Field;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.member.MemberKind;
import dyvil.tools.compiler.ast.method.ICallableMember;
import dyvil.tools.compiler.ast.modifiers.ModifierList;
import dyvil.tools.compiler.ast.modifiers.ModifierSet;
import dyvil.tools.compiler.ast.modifiers.ModifierUtil;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.backend.ClassWriter;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.util.Markers;
import dyvil.lang.Name;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.source.position.SourcePosition;

public class ClassParameter extends Field implements IParameter
{
	// Metadata
	protected int     index;
	protected int     localIndex;
	protected IType   covariantType;

	protected ICallableMember constructor;

	public ClassParameter(IClass enclosingClass)
	{
		super(enclosingClass);
	}

	public ClassParameter(IClass enclosingClass, Name name)
	{
		super(enclosingClass, name);
	}

	public ClassParameter(IClass enclosingClass, Name name, IType type)
	{
		super(enclosingClass, name, type);
	}

	public ClassParameter(IClass enclosingClass, SourcePosition position, Name name, IType type, ModifierSet modifiers,
		                     AnnotationList annotations)
	{
		super(enclosingClass, position, name, type, modifiers == null ? new ModifierList() : modifiers, annotations);
	}

	@Override
	public Name getLabel()
	{
		return this.name;
	}

	@Override
	public void setLabel(Name name)
	{
	}

	@Override
	public String getQualifiedLabel()
	{
		return this.getInternalName();
	}

	@Override
	public MemberKind getKind()
	{
		return MemberKind.CLASS_PARAMETER;
	}

	@Override
	public boolean isLocal()
	{
		return false;
	}

	@Override
	public boolean isAssigned()
	{
		return true;
	}

	@Override
	public ICallableMember getMethod()
	{
		return this.constructor;
	}

	@Override
	public void setMethod(ICallableMember method)
	{
		this.constructor = method;
	}

	@Override
	public IType getInternalType()
	{
		return this.type;
	}

	@Override
	public IDataMember capture(IContext context)
	{
		return this;
	}

	@Override
	public IType getCovariantType()
	{
		if (this.covariantType != null)
		{
			return this.covariantType;
		}

		return this.covariantType = this.type.asParameterType();
	}

	@Override
	public void setVarargs()
	{
		this.modifiers.addIntModifier(Modifiers.VARARGS);
	}

	@Override
	public int getIndex()
	{
		return this.index;
	}

	@Override
	public void setIndex(int index)
	{
		this.index = index;
	}

	@Override
	public int getLocalIndex()
	{
		return this.localIndex;
	}

	@Override
	public void setLocalIndex(int index)
	{
		this.localIndex = index;
	}

	@Override
	public IValue checkAccess(MarkerList markers, SourcePosition position, IValue receiver, IContext context)
	{
		if (receiver != null)
		{
			if (this.hasModifier(Modifiers.STATIC))
			{
				if (receiver.isClassAccess())
				{
					markers.add(Markers.semantic(position, "classparameter.access.static", this.name.unqualified));
					return null;
				}
			}
			else if (receiver.isClassAccess())
			{
				markers.add(Markers.semantic(position, "classparameter.access.instance", this.name.unqualified));
			}
		}
		else if (!this.hasModifier(Modifiers.STATIC))
		{
			markers.add(Markers.semantic(position, "classparameter.access.unqualified", this.name.unqualified));
			return new ThisExpr(position, this.enclosingClass.getThisType(), context, markers);
		}

		ModifierUtil.checkVisibility(this, position, markers, context);

		return receiver;
	}

	@Override
	public IValue checkAssign(MarkerList markers, IContext context, SourcePosition position, IValue receiver,
		                         IValue newValue)
	{
		if (this.enclosingClass.isAnnotation())
		{
			markers.add(Markers.semanticError(position, "classparameter.assign.annotation", this.name.unqualified));
		}
		return super.checkAssign(markers, context, position, receiver, newValue);
	}

	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		if (this.property != null)
		{
			this.property.getModifiers().addIntModifier(Modifiers.PUBLIC);
		}

		super.resolveTypes(markers, context);
	}

	@Override
	public void resolve(MarkerList markers, IContext context)
	{
		super.resolve(markers, context);

		if (this.value !=null)
		{
			this.getModifiers().addIntModifier(Modifiers.DEFAULT);
		}
	}

	@Override
	protected boolean hasDefaultInit()
	{
		return true;
	}

	@Override
	public void write(ClassWriter writer) throws BytecodeException
	{
		if (this.enclosingClass.isAnnotation())
		{
			return;
		}

		super.write(writer);

		if (this.hasModifier(Modifiers.DEFAULT))
		{
			this.writeDefaultValue(writer);
		}
	}

	@Override
	public void writeParameter(MethodWriter writer)
	{
		IParameter.super.writeParameter(writer);
	}

	@Override
	public void writeInit(MethodWriter writer) throws BytecodeException
	{
		IParameter.super.writeInit(writer);
	}
}
