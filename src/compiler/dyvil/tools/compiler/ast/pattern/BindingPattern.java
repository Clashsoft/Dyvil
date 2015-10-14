package dyvil.tools.compiler.ast.pattern;

import dyvil.tools.asm.Label;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.field.Variable;
import dyvil.tools.compiler.ast.member.Name;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public final class BindingPattern extends Pattern
{
	private Name		name;
	private Variable	variable;
	private IType		type	= Types.UNKNOWN;
	
	public BindingPattern(ICodePosition position, Name name)
	{
		this.name = name;
		this.position = position;
	}
	
	@Override
	public int getPatternType()
	{
		return BINDING;
	}
	
	@Override
	public boolean isExhaustive()
	{
		return true;
	}
	
	@Override
	public IType getType()
	{
		return this.type;
	}
	
	@Override
	public IPattern withType(IType type, MarkerList markers)
	{
		if (this.type == Types.ANY || this.type == Types.UNKNOWN)
		{
			this.type = type;
			return this;
		}
		return type.isSuperTypeOf(this.type) ? this : null;
	}
	
	@Override
	public boolean isType(IType type)
	{
		return true;
	}
	
	@Override
	public IDataMember resolveField(Name name)
	{
		if (name != this.name)
		{
			return null;
		}
		
		if (this.variable != null)
		{
			return this.variable;
		}
		
		this.variable = new Variable(this.position, this.name, this.type);
		return this.variable;
	}
	
	@Override
	public boolean isSwitchable()
	{
		return true;
	}
	
	@Override
	public boolean switchCheck()
	{
		return true;
	}
	
	@Override
	public void writeJump(MethodWriter writer, int varIndex, Label elseLabel) throws BytecodeException
	{
		if (this.variable != null)
		{
			this.writeVar(writer, varIndex);
		}
	}
	
	@Override
	public void writeInvJump(MethodWriter writer, int varIndex, Label elseLabel) throws BytecodeException
	{
		if (this.variable != null)
		{
			this.writeVar(writer, varIndex);
		}
	}
	
	private void writeVar(MethodWriter writer, int varIndex) throws BytecodeException
	{
		this.variable.setType(this.type);
		if (varIndex >= 0)
		{
			writer.writeVarInsn(this.type.getLoadOpcode(), varIndex);
		}
		this.variable.writeInit(writer, null);
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append("var ");
		buffer.append(this.name);
	}
}
