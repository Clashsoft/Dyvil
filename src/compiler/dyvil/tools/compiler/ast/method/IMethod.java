package dyvil.tools.compiler.ast.method;

import dyvil.tools.asm.Label;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.generic.GenericData;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeParametric;
import dyvil.tools.compiler.ast.member.IClassMember;
import dyvil.tools.compiler.ast.member.MemberKind;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.parameter.IParameter;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public interface IMethod extends IClassMember, ICallableMember, ICallableSignature, ITypeParametric, IContext
{
	@Override
	default MemberKind getKind()
	{
		return MemberKind.METHOD;
	}

	float getSignatureMatch(Name name, IValue instance, IArguments arguments);
	
	IValue checkArguments(MarkerList markers, ICodePosition position, IContext context, IValue instance, IArguments arguments, ITypeContext typeContext);
	
	void checkCall(MarkerList markers, ICodePosition position, IContext context, IValue instance, IArguments arguments, ITypeContext typeContext);
	
	// Misc
	
	boolean isAbstract();

	boolean hasSideEffects();

	void setHasSideEffects(boolean sideEffects);
	
	void setParameters(IParameter[] parameters, int parameterCount);

	/**
	 * Checks if this method overrides the given {@code candidate} method.
	 *
	 * @param markers
	 * 		the marker list for error reporting
	 * @param iclass
	 * 		the class that is being checked
	 * @param candidate
	 * 		the potential super-method
	 * @param typeContext
	 * 		the type context for type specialization
	 *
	 * @return {@code true}, if this method overrides the given candidate
	 */
	boolean checkOverride(MarkerList markers, IClass iclass, IMethod candidate, ITypeContext typeContext);
	
	// Generics
	
	GenericData getGenericData(GenericData data, IValue instance, IArguments arguments);
	
	boolean hasTypeVariables();
	
	// Compilation
	
	boolean isIntrinsic();
	
	int getInvokeOpcode();
	
	String getDescriptor();
	
	String getSignature();
	
	String[] getInternalExceptions();
	
	void writeCall(MethodWriter writer, IValue instance, IArguments arguments, ITypeContext typeContext, IType targetType, int lineNumber)
			throws BytecodeException;
	
	void writeInvoke(MethodWriter writer, IValue instance, IArguments arguments, ITypeContext typeContext, int lineNumber)
			throws BytecodeException;
	
	void writeJump(MethodWriter writer, Label dest, IValue instance, IArguments arguments, ITypeContext typeContext, int lineNumber)
			throws BytecodeException;
	
	void writeInvJump(MethodWriter writer, Label dest, IValue instance, IArguments arguments, ITypeContext typeContext, int lineNumber)
			throws BytecodeException;
}
