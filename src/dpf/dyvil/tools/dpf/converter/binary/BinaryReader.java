package dyvil.tools.dpf.converter.binary;

import dyvil.tools.dpf.visitor.*;
import dyvil.tools.parsing.Name;

import java.io.DataInput;
import java.io.IOException;

import static dyvil.tools.dpf.converter.binary.BinaryConstants.*;

public class BinaryReader
{
	private final DataInput dataInput;

	public BinaryReader(DataInput dataInput)
	{
		this.dataInput = dataInput;
	}

	public void readValue(ValueVisitor valueVisitor) throws IOException
	{
		this.readValue(this.dataInput.readByte(), valueVisitor);
	}

	private void readValue(int tag, ValueVisitor valueVisitor) throws IOException
	{
		switch (tag)
		{
		case BOOLEAN:
		case BYTE:
		case SHORT:
		case CHAR:
		case INT:
			valueVisitor.visitInt(this.dataInput.readInt());
			return;
		case LONG:
			valueVisitor.visitLong(this.dataInput.readLong());
			return;
		case FLOAT:
			valueVisitor.visitFloat(this.dataInput.readFloat());
			return;
		case DOUBLE:
			valueVisitor.visitDouble(this.dataInput.readDouble());
			return;
		case STRING:
			valueVisitor.visitString(this.dataInput.readUTF());
			return;
		case STRING_INTERPOLATION:
			this.readStringInterpolation(valueVisitor.visitStringInterpolation());
			return;
		case NAME:
			valueVisitor.visitName(this.readName());
			return;
		case NAME_ACCESS:
			this.readValue(valueVisitor.visitValueAccess(this.readName()));
			return;
		case BUILDER:
			this.readBuilder(valueVisitor.visitBuilder(this.readName()));
			return;
		case LIST:
			this.readList(valueVisitor.visitList());
			return;
		case MAP:
			this.readMap(valueVisitor.visitMap());
			return;
		}
	}

	private Name readName() throws IOException
	{
		return Name.getSpecial(this.dataInput.readUTF());
	}

	private void readStringInterpolation(StringInterpolationVisitor stringInterpolationVisitor) throws IOException
	{
		while (true)
		{
			stringInterpolationVisitor.visitStringPart(this.dataInput.readUTF());
			final int tag = this.dataInput.readByte();
			if (tag == END)
			{
				stringInterpolationVisitor.visitEnd();
				return;
			}
			this.readValue(tag, stringInterpolationVisitor.visitValue());
		}
	}

	private void readList(ListVisitor listVisitor) throws IOException
	{
		while (true)
		{
			final int tag = this.dataInput.readByte();
			if (tag == END)
			{
				listVisitor.visitEnd();
				return;
			}
			this.readValue(tag, listVisitor.visitElement());
		}
	}

	private void readMap(MapVisitor mapVisitor) throws IOException
	{
		while (true)
		{
			final int tag = this.dataInput.readByte();
			if (tag == END)
			{
				mapVisitor.visitEnd();
				return;
			}

			this.readValue(tag, mapVisitor.visitKey());
			this.readValue(mapVisitor.visitValue());
		}
	}

	private void readBuilder(BuilderVisitor builderVisitor) throws IOException
	{
		while (true)
		{
			final String param = this.dataInput.readUTF();
			final Name paramName = param.isEmpty() ? null : Name.getSpecial(param);

			final int tag = this.dataInput.readByte();
			if (tag == END)
			{
				this.readNodes(builderVisitor.visitNode());
				builderVisitor.visitEnd();
				this.dataInput.readByte(); // consume the builder end 0
				return;
			}

			this.readValue(tag, builderVisitor.visitParameter(paramName));
		}
	}

	public void readNodes(NodeVisitor nodeVisitor) throws IOException
	{
		while (true)
		{
			final int tag = this.dataInput.readByte();
			if (tag == END)
			{
				nodeVisitor.visitEnd();
				return;
			}
			this.readNode(tag, nodeVisitor);
		}
	}

	public void readNode(NodeVisitor nodeVisitor) throws IOException
	{
		this.readNode(this.dataInput.readByte(), nodeVisitor);
	}

	private void readNode(int tag, NodeVisitor nodeVisitor) throws IOException
	{
		switch (tag)
		{
		case NODE:
			this.readNodes(nodeVisitor.visitNode(this.readName()));
			//           ^ note the s
			return;
		case NODE_ACCESS:
			this.readNode(nodeVisitor.visitNodeAccess(this.readName()));
			return;
		case PROPERTY:
			this.readValue(nodeVisitor.visitProperty(this.readName()));
			return;
		}
	}
}
