package dyvil.tools.compiler.ast.structure;

import java.util.Map;

import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.classes.IClassList;
import dyvil.tools.compiler.ast.imports.ImportDeclaration;
import dyvil.tools.compiler.ast.imports.IncludeDeclaration;
import dyvil.tools.compiler.ast.imports.PackageDecl;
import dyvil.tools.compiler.ast.member.IClassCompilable;
import dyvil.tools.compiler.ast.member.Name;
import dyvil.tools.compiler.ast.operator.IOperatorMap;
import dyvil.tools.compiler.ast.operator.Operator;

public interface IDyvilHeader extends IContext, IClassList, IOperatorMap
{
	public String getName();
	
	// Package
	
	public void setPackage(Package pack);
	
	public Package getPackage();
	
	// Package Declaration
	
	public void setPackageDeclaration(PackageDecl pack);
	
	public PackageDecl getPackageDeclaration();
	
	// Include
	
	public void addImport(ImportDeclaration component);
	
	public void addStaticImport(ImportDeclaration component);
	
	public boolean hasStaticImports();
	
	public void addInclude(IncludeDeclaration component);
	
	// Operators
	
	public Map<Name, Operator> getOperators();
	
	@Override
	public Operator getOperator(Name name);
	
	@Override
	public void addOperator(Operator op);
	
	// Classes
	
	@Override
	public int classCount();
	
	@Override
	public void addClass(IClass iclass);
	
	@Override
	public IClass getClass(int index);
	
	@Override
	public IClass getClass(Name name);
	
	public int innerClassCount();
	
	public void addInnerClass(IClassCompilable iclass);
	
	public IClassCompilable getInnerClass(int index);
	
	// Compilation
	
	public String getInternalName(String subClass);
	
	public String getFullName(String subClass);
}
