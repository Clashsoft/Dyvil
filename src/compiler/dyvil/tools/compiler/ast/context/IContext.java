package dyvil.tools.compiler.ast.context;

import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IAccessible;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.field.IVariable;
import dyvil.tools.compiler.ast.generic.ITypeParameter;
import dyvil.tools.compiler.ast.member.IClassMember;
import dyvil.tools.compiler.ast.method.ConstructorMatchList;
import dyvil.tools.compiler.ast.method.IConstructor;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MethodMatchList;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.structure.IDyvilHeader;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Types;
import dyvil.tools.compiler.backend.IClassCompilable;
import dyvil.tools.parsing.Name;

public interface IContext
{
	byte VISIBLE   = 0;
	byte INVISIBLE = 1;
	byte INTERNAL  = 2;
	
	boolean isStatic();
	
	IDyvilHeader getHeader();
	
	IClass getThisClass();

	IType getThisType();
	
	Package resolvePackage(Name name);
	
	IClass resolveClass(Name name);
	
	IType resolveType(Name name);
	
	ITypeParameter resolveTypeVariable(Name name);
	
	IDataMember resolveField(Name name);
	
	void getMethodMatches(MethodMatchList list, IValue instance, Name name, IArguments arguments);
	
	void getConstructorMatches(ConstructorMatchList list, IArguments arguments);
	
	boolean handleException(IType type);
	
	boolean isMember(IVariable variable);
	
	IDataMember capture(IVariable capture);
	
	IAccessible getAccessibleThis(IClass type);
	
	IValue getImplicit();
	
	static void addCompilable(IContext context, IClassCompilable compilable)
	{
		IClass iclass = context.getThisClass();
		if (iclass != null)
		{
			iclass.addCompilable(compilable);
			return;
		}
		
		IDyvilHeader header = context.getHeader();
		header.addInnerClass(compilable);
	}
	
	static IClass resolveClass(IContext context, Name name)
	{
		final IClass theClass = context.resolveClass(name);
		if (theClass != null)
		{
			return theClass;
		}
		
		return Types.LANG_HEADER.resolveClass(name);
	}
	
	static IType resolveType(IContext context, Name name)
	{
		IType itype = context.resolveType(name);
		if (itype != null)
		{
			return itype;
		}

		return Types.LANG_HEADER.resolveType(name);
	}
	
	static IConstructor resolveConstructor(IContext context, IArguments arguments)
	{
		ConstructorMatchList matches = new ConstructorMatchList();
		context.getConstructorMatches(matches, arguments);
		return matches.getBestConstructor();
	}
	
	static IMethod resolveMethod(IContext context, IValue instance, Name name, IArguments arguments)
	{
		MethodMatchList matches = new MethodMatchList();
		context.getMethodMatches(matches, instance, name, arguments);
		return matches.getBestMethod();
	}
	
	static byte getVisibility(IContext context, IClassMember member)
	{
		IClass thisClass = context.getThisClass();
		if (thisClass != null)
		{
			return thisClass.getVisibility(member);
		}
		
		return context.getHeader().getVisibility(member);
	}

	static boolean isUnhandled(IContext context, IType exceptionType)
	{
		return Types.EXCEPTION.isSuperTypeOf(exceptionType) && !Types.RUNTIME_EXCEPTION.isSuperTypeOf(exceptionType)
				&& !context.handleException(exceptionType);
	}
}
