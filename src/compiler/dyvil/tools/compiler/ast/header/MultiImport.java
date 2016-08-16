package dyvil.tools.compiler.ast.header;

import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MatchList;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class MultiImport extends Import implements IImportContext, IImportList
{
	private IImport[] imports = new IImport[2];
	private int importCount;

	public MultiImport()
	{
		super(null);
	}

	public MultiImport(ICodePosition position)
	{
		super(position);
	}

	@Override
	public int importTag()
	{
		return MULTI;
	}

	@Override
	public int importCount()
	{
		return this.importCount;
	}

	@Override
	public IImport getImport(int index)
	{
		return this.imports[index];
	}

	@Override
	public void setImport(int index, IImport iimport)
	{
		this.imports[index] = iimport;
	}

	@Override
	public void addImport(IImport iimport)
	{
		int index = this.importCount++;
		if (index >= this.imports.length)
		{
			IImport[] temp = new IImport[index + 1];
			System.arraycopy(this.imports, 0, temp, 0, this.imports.length);
			this.imports = temp;
		}
		this.imports[index] = iimport;
	}

	@Override
	public void resolveTypes(MarkerList markers, IImportContext context, int mask)
	{
		if (this.parent != null)
		{
			this.parent.resolveTypes(markers, context, KindedImport.PARENT);
			context = this.parent.asParentContext();
		}

		for (int i = 0; i < this.importCount; i++)
		{
			this.imports[i].resolveTypes(markers, context, mask);
		}
	}

	@Override
	public void resolve(MarkerList markers, IImportContext context, int mask)
	{
		if (this.parent != null)
		{
			context = this.parent.asParentContext();
		}

		for (int i = 0; i < this.importCount; i++)
		{
			this.imports[i].resolve(markers, context, mask);
		}
	}

	@Override
	public IImportContext asContext()
	{
		return this;
	}

	@Override
	public IImportContext asParentContext()
	{
		return null;
	}

	@Override
	public Package resolvePackage(Name name)
	{
		for (int i = 0; i < this.importCount; i++)
		{
			Package pack = this.imports[i].asContext().resolvePackage(name);
			if (pack != null)
			{
				return pack;
			}
		}
		return null;
	}

	@Override
	public IClass resolveClass(Name name)
	{
		for (int i = 0; i < this.importCount; i++)
		{
			IClass iclass = this.imports[i].asContext().resolveClass(name);
			if (iclass != null)
			{
				return iclass;
			}
		}
		return null;
	}

	@Override
	public IDataMember resolveField(Name name)
	{
		for (int i = 0; i < this.importCount; i++)
		{
			IDataMember match = this.imports[i].asContext().resolveField(name);
			if (match != null)
			{
				return match;
			}
		}
		return null;
	}

	@Override
	public void getMethodMatches(MatchList<IMethod> list, IValue receiver, Name name, IArguments arguments)
	{
		for (int i = 0; i < this.importCount; i++)
		{
			this.imports[i].asContext().getMethodMatches(list, receiver, name, arguments);
		}
	}

	@Override
	public void getImplicitMatches(MatchList<IMethod> list, IValue value, IType targetType)
	{
		for (int i = 0; i < this.importCount; i++)
		{
			this.imports[i].asContext().getImplicitMatches(list, value, targetType);
		}
	}

	@Override
	public void writeData(DataOutput out) throws IOException
	{
		IImport.writeImport(this.parent, out);

		out.writeShort(this.importCount);
		for (int i = 0; i < this.importCount; i++)
		{
			IImport.writeImport(this.imports[i], out);
		}
	}

	@Override
	public void readData(DataInput in) throws IOException
	{
		this.parent = IImport.readImport(in);

		this.importCount = in.readShort();
		this.imports = new IImport[this.importCount];
		for (int i = 0; i < this.importCount; i++)
		{
			this.imports[i] = IImport.readImport(in);
		}
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		this.appendParent(prefix, buffer);

		Formatting.appendSeparator(buffer, "import.multi.open_brace", '{');
		Util.astToString(prefix, this.imports, this.importCount, Formatting.getSeparator("import.multi.separator", ','),
		                 buffer);

		if (Formatting.getBoolean("import.multi.close_brace.space_before"))
		{
			buffer.append(' ');
		}
		buffer.append('}');
	}
}
