package dyvil.tools.compiler.ast.type;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import dyvil.reflect.Opcodes;
import dyvil.tools.asm.TypeAnnotatableVisitor;
import dyvil.tools.asm.TypePath;
import dyvil.tools.compiler.ast.annotation.IAnnotation;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.expression.LambdaExpression;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeVariable;
import dyvil.tools.compiler.ast.method.ConstructorMatchList;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MethodMatchList;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.util.I18n;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;

public final class LambdaType implements IObjectType, ITyped, ITypeList
{
	public static final IClass[] functionClasses = new IClass[22];
	
	protected IType		returnType;
	protected IType[]	parameterTypes;
	protected int		parameterCount;
	
	public LambdaType()
	{
		this.parameterTypes = new IType[2];
	}
	
	public LambdaType(IType type)
	{
		this.parameterTypes = new IType[1];
		this.parameterTypes[0] = type;
		this.parameterCount = 1;
	}
	
	public LambdaType(TupleType tupleType)
	{
		this.parameterTypes = tupleType.types;
		this.parameterCount = tupleType.typeCount;
	}
	
	public LambdaType(IType[] types, int typeCount, IType returnType)
	{
		this.parameterTypes = types;
		this.parameterCount = typeCount;
		this.returnType = returnType;
	}
	
	public LambdaType(int typeCount)
	{
		this.parameterTypes = new IType[typeCount];
	}
	
	public static IClass getLambdaClass(int typeCount)
	{
		IClass iclass = functionClasses[typeCount];
		if (iclass != null)
		{
			return iclass;
		}
		
		iclass = Package.dyvilFunction.resolveClass("Function" + typeCount);
		functionClasses[typeCount] = iclass;
		return iclass;
	}
	
	@Override
	public int typeTag()
	{
		return LAMBDA;
	}
	
	@Override
	public boolean isGenericType()
	{
		return true;
	}
	
	@Override
	public Name getName()
	{
		return this.getTheClass().getName();
	}
	
	@Override
	public void setType(IType type)
	{
		this.returnType = type;
	}
	
	@Override
	public IType getType()
	{
		return this.returnType;
	}
	
	// ITypeList Overrides
	
	@Override
	public int typeCount()
	{
		return this.parameterCount;
	}
	
	@Override
	public void setType(int index, IType type)
	{
		this.parameterTypes[index] = type;
	}
	
	@Override
	public void addType(IType type)
	{
		int index = this.parameterCount++;
		if (this.parameterCount > this.parameterTypes.length)
		{
			IType[] temp = new IType[this.parameterCount];
			System.arraycopy(this.parameterTypes, 0, temp, 0, index);
			this.parameterTypes = temp;
		}
		this.parameterTypes[index] = type;
	}
	
	@Override
	public IType getType(int index)
	{
		return this.parameterTypes[index];
	}
	
	// IType Overrides
	
	@Override
	public IClass getTheClass()
	{
		return getLambdaClass(this.parameterCount);
	}
	
	@Override
	public int getSubClassDistance(IType subtype)
	{
		if (this.parameterCount == 0)
		{
			int i = this.returnType.getSubClassDistance(subtype);
			if (i != 0)
			{
				return i;
			}
		}
		
		return IObjectType.super.getSubClassDistance(subtype);
	}
	
	@Override
	public float getSubTypeDistance(IType subtype)
	{
		if (this.parameterCount == 0)
		{
			float f = this.returnType.getSubTypeDistance(subtype);
			if (f != 0)
			{
				return f;
			}
		}
		
		return IObjectType.super.getSubTypeDistance(subtype);
	}
	
	@Override
	public boolean isSuperTypeOf(IType type)
	{
		if (this.parameterCount == 0 && this.returnType.isSuperTypeOf(type))
		{
			return true;
		}
		
		if (!IObjectType.super.isSuperTypeOf(type))
		{
			return false;
		}
		
		IClass iclass = this.getTheClass();
		ITypeVariable typeVar = iclass.getTypeVariable(this.parameterCount);
		IType type1 = type.resolveTypeSafely(typeVar);
		
		// Return type is Covariant
		if (!this.returnType.isSuperTypeOf(type1))
		{
			return false;
		}
		
		for (int i = 0; i < this.parameterCount; i++)
		{
			typeVar = iclass.getTypeVariable(i);
			type1 = type.resolveType(typeVar);
			
			// Contravariance
			if (!type1.isSuperTypeOf(this.parameterTypes[i]))
			{
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public IValue convertValue(IValue value, ITypeContext typeContext, MarkerList markers, IContext context)
	{
		if (this.parameterCount != 0 || IObjectType.super.isSuperClassOf(value.getType()))
		{
			return value.withType(this, typeContext, markers, context);
		}
		
		IValue value1 = value.withType(this.returnType.getConcreteType(typeContext), typeContext, markers, context);
		if (value1 != null)
		{
			return this.wrapLambda(value1, typeContext);
		}
		return null;
	}
	
	public LambdaExpression wrapLambda(IValue value, ITypeContext typeContext)
	{
		IType returnType = value.getType();
		
		LambdaExpression le = new LambdaExpression(value.getPosition(), null, 0);
		le.setMethod(this.getFunctionalMethod());
		le.setReturnType(returnType);
		le.setValue(value);
		le.setType(this);
		le.inferReturnType(this, typeContext, returnType);
		return le;
	}
	
	@Override
	public IType resolveType(ITypeVariable typeVar)
	{
		// See TupleType.resolveType(ITypeVariable) for a note about supertype
		// checking
		int index = typeVar.getIndex();
		if (index == this.parameterCount)
		{
			return this.returnType;
		}
		if (index > this.parameterCount)
		{
			return null;
		}
		return this.parameterTypes[index];
	}
	
	@Override
	public boolean hasTypeVariables()
	{
		if (this.returnType.hasTypeVariables())
		{
			return true;
		}
		
		for (int i = 0; i < this.parameterCount; i++)
		{
			if (this.parameterTypes[i].hasTypeVariables())
			{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public IType getConcreteType(ITypeContext context)
	{
		LambdaType lt = new LambdaType(this.parameterCount);
		lt.parameterCount = this.parameterCount;
		for (int i = 0; i < this.parameterCount; i++)
		{
			lt.parameterTypes[i] = this.parameterTypes[i].getConcreteType(context);
		}
		lt.returnType = this.returnType.getConcreteType(context);
		return lt;
	}
	
	@Override
	public void inferTypes(IType concrete, ITypeContext typeContext)
	{
		boolean found = false;
		
		ITypeVariable typeVar;
		IType concreteType;
		IClass iclass = this.getTheClass();
		for (int i = 0; i < this.parameterCount; i++)
		{
			typeVar = iclass.getTypeVariable(i);
			concreteType = concrete.resolveType(typeVar);
			if (concreteType != null)
			{
				this.parameterTypes[i].inferTypes(concreteType, typeContext);
				found = true;
			}
		}
		
		typeVar = iclass.getTypeVariable(this.parameterCount);
		concreteType = concrete.resolveType(typeVar);
		if (concreteType != null)
		{
			this.returnType.inferTypes(concreteType, typeContext);
			found = true;
		}
		
		if (!found && this.parameterCount == 0 && this.returnType.isSuperTypeOf(concrete))
		{
			this.returnType.inferTypes(concrete, typeContext);
		}
	}
	
	@Override
	public boolean isResolved()
	{
		return true;
	}
	
	@Override
	public IType resolveType(MarkerList markers, IContext context)
	{
		for (int i = 0; i < this.parameterCount; i++)
		{
			this.parameterTypes[i] = this.parameterTypes[i].resolveType(markers, context);
		}
		this.returnType = this.returnType.resolveType(markers, context);
		
		return this;
	}
	
	@Override
	public void resolve(MarkerList markers, IContext context)
	{
		for (int i = 0; i < this.parameterCount; i++)
		{
			this.parameterTypes[i].resolve(markers, context);
		}
		this.returnType.resolve(markers, context);
	}
	
	@Override
	public void checkType(MarkerList markers, IContext context, TypePosition position)
	{
		if (position == TypePosition.CLASS)
		{
			markers.add(I18n.createMarker(this.returnType.getPosition(), "type.class.lambda"));
		}
		
		for (int i = 0; i < this.parameterCount; i++)
		{
			this.parameterTypes[i].checkType(markers, context, TypePosition.PARAMETER_TYPE);
		}
		this.returnType.checkType(markers, context, TypePosition.RETURN_TYPE);
	}
	
	@Override
	public void check(MarkerList markers, IContext context)
	{
		for (int i = 0; i < this.parameterCount; i++)
		{
			this.parameterTypes[i].check(markers, context);
		}
		this.returnType.check(markers, context);
	}
	
	@Override
	public void foldConstants()
	{
		for (int i = 0; i < this.parameterCount; i++)
		{
			this.parameterTypes[i].foldConstants();
		}
		this.returnType.foldConstants();
	}
	
	@Override
	public void cleanup(IContext context, IClassCompilableList compilableList)
	{
		for (int i = 0; i < this.parameterCount; i++)
		{
			this.parameterTypes[i].cleanup(context, compilableList);
		}
		this.returnType.cleanup(context, compilableList);
	}
	
	@Override
	public IDataMember resolveField(Name name)
	{
		return null;
	}
	
	@Override
	public void getMethodMatches(MethodMatchList list, IValue instance, Name name, IArguments arguments)
	{
		getLambdaClass(this.parameterCount).getMethodMatches(list, instance, name, arguments);
	}
	
	@Override
	public void getConstructorMatches(ConstructorMatchList list, IArguments arguments)
	{
	}
	
	@Override
	public IMethod getFunctionalMethod()
	{
		return getLambdaClass(this.parameterCount).getFunctionalMethod();
	}
	
	@Override
	public void writeTypeExpression(MethodWriter writer) throws BytecodeException
	{
		this.returnType.writeTypeExpression(writer);
		
		writer.writeLDC(this.parameterCount);
		writer.writeNewArray("dyvil/lang/Type", 1);
		for (int i = 0; i < this.parameterCount; i++)
		{
			writer.writeInsn(Opcodes.DUP);
			writer.writeLDC(i);
			this.parameterTypes[i].writeTypeExpression(writer);
			writer.writeInsn(Opcodes.AASTORE);
		}
		
		writer.writeInvokeInsn(Opcodes.INVOKESTATIC, "dyvil/reflect/types/FunctionType", "apply",
				"(Ldyvil/lang/Type;[Ldyvil/lang/Type;)Ldyvil/reflect/types/FunctionType;", false);
	}
	
	@Override
	public String getInternalName()
	{
		return "dyvil/function/Function" + this.parameterCount;
	}
	
	@Override
	public void appendExtendedName(StringBuilder buffer)
	{
		buffer.append("Ldyvil/function/Function").append(this.parameterCount).append(';');
	}
	
	@Override
	public String getSignature()
	{
		StringBuilder sb = new StringBuilder();
		this.appendSignature(sb);
		return sb.toString();
	}
	
	@Override
	public void appendSignature(StringBuilder buffer)
	{
		buffer.append("Ldyvil/function/Function").append(this.parameterCount).append('<');
		for (int i = 0; i < this.parameterCount; i++)
		{
			this.parameterTypes[i].appendSignature(buffer);
		}
		this.returnType.appendSignature(buffer);
		buffer.append(">;");
	}
	
	@Override
	public void addAnnotation(IAnnotation annotation, TypePath typePath, int step, int steps)
	{
		if (typePath.getStep(step) != TypePath.TYPE_ARGUMENT)
		{
			return;
		}
		
		int index = typePath.getStepArgument(step);
		if (index < this.parameterCount)
		{
			this.parameterTypes[index] = IType.withAnnotation(this.parameterTypes[index], annotation, typePath, step + 1, steps);
			return;
		}
		this.returnType = IType.withAnnotation(this.returnType, annotation, typePath, step + 1, steps);
	}
	
	@Override
	public void writeAnnotations(TypeAnnotatableVisitor visitor, int typeRef, String typePath)
	{
		for (int i = 0; i < this.parameterCount; i++)
		{
			this.parameterTypes[i].writeAnnotations(visitor, typeRef, typePath + i + ';');
		}
		this.returnType.writeAnnotations(visitor, typeRef, typePath + this.parameterCount + ';');
	}
	
	@Override
	public void write(DataOutput out) throws IOException
	{
		out.writeByte(this.parameterCount);
		for (int i = 0; i < this.parameterCount; i++)
		{
			IType.writeType(this.parameterTypes[i], out);
		}
		IType.writeType(this.returnType, out);
	}
	
	@Override
	public void read(DataInput in) throws IOException
	{
		int len = this.parameterCount = in.readByte();
		if (len > this.parameterTypes.length)
		{
			this.parameterTypes = new IType[len];
		}
		for (int i = 0; i < len; i++)
		{
			this.parameterTypes[i] = IType.readType(in);
		}
		this.returnType = IType.readType(in);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		if (this.parameterCount > 0)
		{
			sb.append(this.parameterTypes[0]);
			for (int i = 1; i < this.parameterCount; i++)
			{
				sb.append(", ").append(this.parameterTypes[i]);
			}
		}
		sb.append(") => ").append(this.returnType);
		return sb.toString();
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		if (this.parameterCount == 1)
		{
			if (this.parameterTypes[0].typeTag() == TUPLE)
			{
				buffer.append(Formatting.Method.parametersStart);
				this.parameterTypes[0].toString(prefix, buffer);
				buffer.append(Formatting.Method.parametersEnd);
			}
			else
			{
				this.parameterTypes[0].toString(prefix, buffer);
			}
			buffer.append(' ');
		}
		else if (this.parameterCount > 0)
		{
			buffer.append(Formatting.Method.parametersStart);
			Util.astToString(prefix, this.parameterTypes, this.parameterCount, Formatting.Method.parameterSeperator, buffer);
			buffer.append(Formatting.Method.parametersEnd);
			buffer.append(' ');
		}
		
		buffer.append(Formatting.Expression.lambdaSeperator);
		this.returnType.toString("", buffer);
	}
	
	@Override
	public IType clone()
	{
		LambdaType lt = new LambdaType(this.parameterCount);
		lt.parameterCount = this.parameterCount;
		System.arraycopy(this.parameterTypes, 0, lt.parameterTypes, 0, this.parameterCount);
		lt.returnType = this.returnType;
		return lt;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return IType.equals(this, obj);
	}
	
	@Override
	public int hashCode()
	{
		return System.identityHashCode(getLambdaClass(this.parameterCount));
	}
}
