package dyvil.tools.compiler.ast.structure;

import java.io.File;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IField;
import dyvil.tools.compiler.ast.generic.ITypeVariable;
import dyvil.tools.compiler.ast.imports.ImportDeclaration;
import dyvil.tools.compiler.ast.imports.IncludeDeclaration;
import dyvil.tools.compiler.ast.imports.PackageDeclaration;
import dyvil.tools.compiler.ast.member.IClassCompilable;
import dyvil.tools.compiler.ast.member.IMember;
import dyvil.tools.compiler.ast.member.Name;
import dyvil.tools.compiler.ast.method.ConstructorMatch;
import dyvil.tools.compiler.ast.method.MethodMatch;
import dyvil.tools.compiler.ast.operator.Operator;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.backend.HeaderFile;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.lexer.CodeFile;
import dyvil.tools.compiler.lexer.Dlex;
import dyvil.tools.compiler.lexer.TokenIterator;
import dyvil.tools.compiler.lexer.marker.MarkerList;
import dyvil.tools.compiler.parser.ParserManager;
import dyvil.tools.compiler.parser.classes.DyvilHeaderParser;

public class DyvilHeader implements ICompilationUnit, IDyvilHeader
{
	public final CodeFile			inputFile;
	public final File				outputDirectory;
	public final File				outputFile;
	
	public final String				name;
	public Package					pack;
	
	protected TokenIterator			tokens;
	protected MarkerList			markers			= new MarkerList();
	
	protected PackageDeclaration	packageDeclaration;
	
	protected ImportDeclaration[]	imports			= new ImportDeclaration[5];
	protected int					importCount;
	protected ImportDeclaration[]	staticImports	= new ImportDeclaration[1];
	protected int					staticImportCount;
	protected IncludeDeclaration[]	includes;
	protected int					includeCount;
	
	protected Map<Name, Operator>	operators		= new IdentityHashMap();
	protected Map<Name, Operator>	inheritedOperators;
	
	public DyvilHeader(String name)
	{
		this.inputFile = null;
		this.outputDirectory = null;
		this.outputFile = null;
		this.name = name;
	}
	
	public DyvilHeader(Package pack, CodeFile input, File output)
	{
		this.pack = pack;
		this.inputFile = input;
		
		String name = input.getAbsolutePath();
		int start = name.lastIndexOf('/');
		int end = name.lastIndexOf('.');
		this.name = name.substring(start + 1, end);
		
		name = output.getPath();
		start = name.lastIndexOf('/');
		end = name.lastIndexOf('.');
		this.outputDirectory = new File(name.substring(0, start));
		this.outputFile = new File(name.substring(0, end) + ".dyhbin");
	}
	
	@Override
	public boolean isHeader()
	{
		return true;
	}
	
	@Override
	public String getName()
	{
		return this.name;
	}
	
	@Override
	public CodeFile getInputFile()
	{
		return this.inputFile;
	}
	
	@Override
	public File getOutputFile()
	{
		return this.outputFile;
	}
	
	@Override
	public void setPackage(Package pack)
	{
		this.pack = pack;
	}
	
	@Override
	public Package getPackage()
	{
		return this.pack;
	}
	
	@Override
	public void setPackageDeclaration(PackageDeclaration packageDecl)
	{
		this.packageDeclaration = packageDecl;
	}
	
	@Override
	public PackageDeclaration getPackageDeclaration()
	{
		return this.packageDeclaration;
	}
	
	@Override
	public int importCount()
	{
		return this.importCount;
	}
	
	@Override
	public void addImport(ImportDeclaration component)
	{
		int index = this.importCount++;
		if (index >= this.imports.length)
		{
			ImportDeclaration[] temp = new ImportDeclaration[index + 1];
			System.arraycopy(this.imports, 0, temp, 0, this.imports.length);
			this.imports = temp;
		}
		this.imports[index] = component;
	}
	
	@Override
	public ImportDeclaration getImport(int index)
	{
		return this.imports[index];
	}
	
	@Override
	public int staticImportCount()
	{
		return this.staticImportCount;
	}
	
	@Override
	public void addStaticImport(ImportDeclaration component)
	{
		int index = this.staticImportCount++;
		if (index >= this.staticImports.length)
		{
			ImportDeclaration[] temp = new ImportDeclaration[index + 1];
			System.arraycopy(this.staticImports, 0, temp, 0, this.staticImports.length);
			this.staticImports = temp;
		}
		this.staticImports[index] = component;
	}
	
	@Override
	public ImportDeclaration getStaticImport(int index)
	{
		return this.staticImports[index];
	}
	
	@Override
	public int includeCount()
	{
		return this.includeCount;
	}
	
	protected void addIncludeToArray(IncludeDeclaration component)
	{
		if (this.includes == null)
		{
			this.includes = new IncludeDeclaration[2];
			this.includes[0] = component;
			this.includeCount = 1;
			
			if (!this.isHeader())
			{
				this.inheritedOperators = new IdentityHashMap();
			}
		}
		else
		{
			int index = this.includeCount++;
			if (index >= this.includes.length)
			{
				IncludeDeclaration[] temp = new IncludeDeclaration[index + 1];
				System.arraycopy(this.includes, 0, temp, 0, this.includes.length);
				this.includes = temp;
			}
			this.includes[index] = component;
		}
	}
	
	@Override
	public void addInclude(IncludeDeclaration component)
	{
		this.addIncludeToArray(component);
		
		if (this.isHeader())
		{
			this.markers.add(component.getPosition(), "header.include");
			return;
		}
		
		// Resolve the header
		
		component.resolve(this.markers);
		component.addOperators(this.inheritedOperators);
	}
	
	@Override
	public IncludeDeclaration getInclude(int index)
	{
		return this.includes[index];
	}
	
	@Override
	public boolean hasStaticImports()
	{
		return this.staticImportCount > 0 || this.includeCount > 0;
	}
	
	@Override
	public Map<Name, Operator> getOperators()
	{
		return this.operators;
	}
	
	@Override
	public void addOperator(Operator op)
	{
		this.operators.put(op.name, op);
	}
	
	@Override
	public Operator getOperator(Name name)
	{
		Operator op1 = this.operators.get(name);
		if (op1 != null)
		{
			return op1;
		}
		if (this.inheritedOperators == null)
		{
			return null;
		}
		return this.inheritedOperators.get(op1);
	}
	
	@Override
	public int classCount()
	{
		return 0;
	}
	
	@Override
	public IClass getClass(Name name)
	{
		return null;
	}
	
	@Override
	public void addClass(IClass iclass)
	{
	}
	
	@Override
	public IClass getClass(int index)
	{
		return null;
	}
	
	@Override
	public int innerClassCount()
	{
		return 0;
	}
	
	@Override
	public void addInnerClass(IClassCompilable iclass)
	{
	}
	
	@Override
	public IClassCompilable getInnerClass(int index)
	{
		return null;
	}
	
	@Override
	public void tokenize()
	{
		this.tokens = Dlex.tokenIterator(this.inputFile.getCode());
		this.tokens.inferSemicolons();
	}
	
	@Override
	public void parse()
	{
		ParserManager manager = new ParserManager(new DyvilHeaderParser(this));
		manager.setOperatorMap(this);
		manager.parse(this.markers, this.tokens);
		this.tokens = null;
	}
	
	@Override
	public void resolveTypes()
	{
		for (int i = 0; i < this.importCount; i++)
		{
			this.imports[i].resolveTypes(this.markers, this, false);
		}
		
		for (int i = 0; i < this.staticImportCount; i++)
		{
			this.staticImports[i].resolveTypes(this.markers, this, true);
		}
	}
	
	@Override
	public void resolve()
	{
	}
	
	@Override
	public void checkTypes()
	{
	}
	
	@Override
	public void check()
	{
		this.pack.check(this.packageDeclaration, this.inputFile, this.markers);
	}
	
	@Override
	public void foldConstants()
	{
	}
	
	@Override
	public void compile()
	{
		if (ICompilationUnit.printMarkers(this.markers, "Dyvil Header", this.name, this.inputFile))
		{
			return;
		}
		
		HeaderFile.write(this.outputFile, this);
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
		IClass iclass;
		
		// Imported Classes
		for (int i = 0; i < this.importCount; i++)
		{
			iclass = this.imports[i].resolveClass(name);
			if (iclass != null)
			{
				return iclass;
			}
		}
		
		// Included Headers
		for (int i = 0; i < this.includeCount; i++)
		{
			iclass = this.includes[i].getHeader().resolveClass(name);
			if (iclass != null)
			{
				return iclass;
			}
		}
		
		if (this.pack != null)
		{
			return this.pack.resolveClass(name);
		}
		return null;
	}
	
	@Override
	public ITypeVariable resolveTypeVariable(Name name)
	{
		return null;
	}
	
	@Override
	public IField resolveField(Name name)
	{
		for (int i = 0; i < this.staticImportCount; i++)
		{
			IField field = this.staticImports[i].resolveField(name);
			if (field != null)
			{
				return field;
			}
		}
		
		for (int i = 0; i < this.includeCount; i++)
		{
			IField field = this.includes[i].getHeader().resolveField(name);
			if (field != null)
			{
				return field;
			}
		}
		return null;
	}
	
	@Override
	public void getMethodMatches(List<MethodMatch> list, IValue instance, Name name, IArguments arguments)
	{
		for (int i = 0; i < this.staticImportCount; i++)
		{
			this.staticImports[i].getMethodMatches(list, instance, name, arguments);
		}
		
		for (int i = 0; i < this.includeCount; i++)
		{
			this.includes[i].getHeader().getMethodMatches(list, instance, name, arguments);
		}
	}
	
	@Override
	public void getConstructorMatches(List<ConstructorMatch> list, IArguments arguments)
	{
	}
	
	@Override
	public boolean handleException(IType type)
	{
		return false;
	}
	
	@Override
	public byte getVisibility(IMember member)
	{
		return 0;
	}
	
	@Override
	public String getFullName()
	{
		return this.pack.fullName + '.' + this.name;
	}
	
	@Override
	public String getFullName(String name)
	{
		if (!name.equals(this.name))
		{
			name = this.name + '.' + name;
		}
		return this.pack.fullName + '.' + name;
	}
	
	@Override
	public String getInternalName()
	{
		return this.pack.internalName + this.name;
	}
	
	@Override
	public String getInternalName(String name)
	{
		if (!name.equals(this.name))
		{
			name = this.name + '$' + name;
		}
		return this.pack.internalName + name;
	}
	
	@Override
	public String toString()
	{
		StringBuilder buf = new StringBuilder();
		this.toString("", buf);
		return buf.toString();
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		if (this.packageDeclaration != null)
		{
			buffer.append(prefix);
			this.packageDeclaration.toString(prefix, buffer);
			buffer.append(";\n");
			if (Formatting.Package.newLine)
			{
				buffer.append('\n');
			}
		}
		
		if (this.includeCount > 0)
		{
			for (int i = 0; i < this.includeCount; i++)
			{
				buffer.append(prefix);
				this.includes[i].toString(prefix, buffer);
				buffer.append(";\n");
			}
			buffer.append('\n');
		}
		
		if (this.importCount > 0)
		{
			for (int i = 0; i < this.importCount; i++)
			{
				buffer.append(prefix);
				this.imports[i].toString(prefix, buffer);
				buffer.append(";\n");
			}
			if (Formatting.Import.newLine)
			{
				buffer.append('\n');
			}
		}
		
		if (this.staticImportCount > 0)
		{
			for (int i = 0; i < this.staticImportCount; i++)
			{
				buffer.append(prefix);
				this.staticImports[i].toString(prefix, buffer);
				buffer.append(";\n");
			}
			if (Formatting.Import.newLine)
			{
				buffer.append('\n');
			}
		}
		
		if (!this.operators.isEmpty())
		{
			for (Entry<Name, Operator> entry : this.operators.entrySet())
			{
				buffer.append(prefix);
				entry.getValue().toString(buffer);
				buffer.append(";\n");
			}
			if (Formatting.Import.newLine)
			{
				buffer.append('\n');
			}
		}
	}
}
