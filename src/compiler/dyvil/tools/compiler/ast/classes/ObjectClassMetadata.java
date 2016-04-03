package dyvil.tools.compiler.ast.classes;

import dyvil.reflect.Modifiers;
import dyvil.reflect.Opcodes;
import dyvil.tools.asm.Label;
import dyvil.tools.compiler.ast.access.ConstructorCall;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.field.Field;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.field.IField;
import dyvil.tools.compiler.ast.modifiers.FlagModifierSet;
import dyvil.tools.compiler.ast.parameter.EmptyArguments;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.ClassWriter;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.MethodWriterImpl;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.transform.Names;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;

public final class ObjectClassMetadata extends ClassMetadata
{
	protected IField instanceField;
	
	public ObjectClassMetadata(IClass iclass)
	{
		super(iclass);
	}
	
	@Override
	public IDataMember getInstanceField()
	{
		return this.instanceField;
	}
	
	@Override
	public void resolveTypesHeader(MarkerList markers, IContext context)
	{
		super.resolveTypesHeader(markers, context);
		
		if (!this.theClass.isSubClassOf(Types.SERIALIZABLE))
		{
			this.theClass.addInterface(Types.SERIALIZABLE);
		}
	}
	
	@Override
	public void resolveTypesBody(MarkerList markers, IContext context)
	{
		super.resolveTypesBody(markers, context);
		
		this.checkMethods();
		
		final IClassBody body = this.theClass.getBody();
		if (body != null)
		{
			if (markers != null && body.constructorCount() > 0)
			{
				markers.add(Markers.semantic(this.theClass.getPosition(), "class.object.constructor",
				                             this.theClass.getName().qualified));
			}
			
			final IField field = body.getField(Names.instance);
			if (field != null)
			{
				this.members |= INSTANCE_FIELD;
				this.instanceField = field;
			}
		}
	}

	@Override
	public void resolveTypesGenerate(MarkerList markers, IContext context)
	{
		super.resolveTypesGenerate(markers, context);

		final Field field = new Field(this.theClass, Names.instance, this.theClass.getType(),
		                              new FlagModifierSet(Modifiers.PUBLIC | Modifiers.CONST));
		this.instanceField = field;
		
		this.constructor.setModifiers(new FlagModifierSet(Modifiers.PRIVATE));
		ConstructorCall call = new ConstructorCall(null, this.constructor, EmptyArguments.INSTANCE);
		field.setValue(call);
	}
	
	@Override
	public IDataMember resolveField(Name name)
	{
		if (this.instanceField != null && name == Names.instance)
		{
			return this.instanceField;
		}
		return null;
	}
	
	@Override
	public void writeStaticInit(MethodWriter mw) throws BytecodeException
	{
		if (this.instanceField != null)
		{
			this.instanceField.writeStaticInit(mw);
		}
	}
	
	@Override
	public void write(ClassWriter writer) throws BytecodeException
	{
		if (this.instanceField != null)
		{
			this.instanceField.write(writer);
		}
		
		super.write(writer);
		
		String internalName = this.theClass.getInternalName();
		if ((this.members & TOSTRING) == 0)
		{
			// Generate a toString() method that simply returns the name of this
			// object type.
			MethodWriterImpl mw = new MethodWriterImpl(writer, writer.visitMethod(Modifiers.PUBLIC, "toString",
			                                                                      "()Ljava/lang/String;", null, null));
			mw.visitCode();
			mw.setThisType(internalName);
			mw.visitLdcInsn(this.theClass.getName().unqualified);
			mw.visitInsn(Opcodes.ARETURN);
			mw.visitEnd(Types.STRING);
		}
		
		if ((this.members & EQUALS) == 0)
		{
			// Generate an equals(Object) method that compares the objects for
			// identity
			MethodWriterImpl mw = new MethodWriterImpl(writer, writer.visitMethod(Modifiers.PUBLIC, "equals",
			                                                                      "(Ljava/lang/Object;)Z", null, null));
			mw.visitCode();
			mw.setThisType(internalName);
			mw.visitParameter(1, "obj", Types.ANY, 0);
			mw.visitVarInsn(Opcodes.ALOAD, 0);
			mw.visitVarInsn(Opcodes.ALOAD, 1);
			Label label = new Label();
			mw.visitJumpInsn(Opcodes.IF_ACMPNE, label);
			mw.visitLdcInsn(1);
			mw.visitInsn(Opcodes.IRETURN);
			mw.visitLabel(label);
			mw.visitLdcInsn(0);
			mw.visitInsn(Opcodes.IRETURN);
			mw.visitEnd();
		}
		
		if ((this.members & HASHCODE) == 0)
		{
			MethodWriterImpl mw = new MethodWriterImpl(writer,
			                                           writer.visitMethod(Modifiers.PUBLIC, "hashCode", "()I", null,
			                                                              null));
			mw.visitCode();
			mw.setThisType(internalName);
			mw.visitLdcInsn(internalName.hashCode());
			mw.visitInsn(Opcodes.IRETURN);
			mw.visitEnd();
		}
		
		if ((this.members & READ_RESOLVE) == 0)
		{
			MethodWriterImpl mw = new MethodWriterImpl(writer,
			                                           writer.visitMethod(Modifiers.PRIVATE | Modifiers.SYNTHETIC,
			                                                              "readResolve", "()Ljava/lang/Object;", null,
			                                                              null));
			writeResolveMethod(mw, internalName);
		}
		
		if ((this.members & WRITE_REPLACE) == 0)
		{
			MethodWriterImpl mw = new MethodWriterImpl(writer,
			                                           writer.visitMethod(Modifiers.PRIVATE | Modifiers.SYNTHETIC,
			                                                              "writeReplace", "()Ljava/lang/Object;", null,
			                                                              null));
			writeResolveMethod(mw, internalName);
		}
	}
	
	private static void writeResolveMethod(MethodWriter mw, String internal) throws BytecodeException
	{
		mw.setThisType(internal);
		mw.visitCode();
		mw.visitFieldInsn(Opcodes.GETSTATIC, internal, "instance", 'L' + internal + ';');
		mw.visitInsn(Opcodes.ARETURN);
		mw.visitEnd();
	}
}
