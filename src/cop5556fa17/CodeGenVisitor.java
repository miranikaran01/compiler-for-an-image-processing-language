package cop5556fa17;

import java.util.ArrayList;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

//import cop5556fa17.image.ImageFrame;
//import cop5556fa17.image.ImageSupport;
import cop5556fa17.Scanner.Kind;
import cop5556fa17.TypeUtils.Type;
import cop5556fa17.AST.ASTNode;
import cop5556fa17.AST.ASTVisitor;
import cop5556fa17.AST.Declaration_Image;
import cop5556fa17.AST.Declaration_SourceSink;
import cop5556fa17.AST.Declaration_Variable;
import cop5556fa17.AST.Expression;
import cop5556fa17.AST.Expression_Binary;
import cop5556fa17.AST.Expression_BooleanLit;
import cop5556fa17.AST.Expression_Conditional;
import cop5556fa17.AST.Expression_FunctionAppWithExprArg;
import cop5556fa17.AST.Expression_FunctionAppWithIndexArg;
import cop5556fa17.AST.Expression_Ident;
import cop5556fa17.AST.Expression_IntLit;
import cop5556fa17.AST.Expression_PixelSelector;
import cop5556fa17.AST.Expression_PredefinedName;
import cop5556fa17.AST.Expression_Unary;
import cop5556fa17.AST.Index;
import cop5556fa17.AST.LHS;
import cop5556fa17.AST.Program;
import cop5556fa17.AST.Sink_Ident;
import cop5556fa17.AST.Sink_SCREEN;
import cop5556fa17.AST.Source;
import cop5556fa17.AST.Source_CommandLineParam;
import cop5556fa17.AST.Source_Ident;
import cop5556fa17.AST.Source_StringLiteral;
import cop5556fa17.AST.Statement_Assign;
import cop5556fa17.AST.Statement_In;
import cop5556fa17.AST.Statement_Out;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	/**
	 * All methods and variable static.
	 */

	/**
	 * @param DEVEL
	 *            used as parameter to genPrint and genPrintTOS
	 * @param GRADE
	 *            used as parameter to genPrint and genPrintTOS
	 * @param sourceFileName
	 *            name of source file, may be null.
	 */
	public CodeGenVisitor(boolean DEVEL, boolean GRADE, String sourceFileName) {
		super();
		this.DEVEL = DEVEL;
		this.GRADE = GRADE;
		this.sourceFileName = sourceFileName;
	}

	ClassWriter cw;
	String className;
	String classDesc;
	String sourceFileName;

	MethodVisitor mv; // visitor of method currently under construction
	FieldVisitor fv;
	/** Indicates whether genPrint and genPrintTOS should generate code. */
	final boolean DEVEL;
	final boolean GRADE;

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		className = program.name;
		classDesc = "L" + className + ";";
		String sourceFileName = (String) arg;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);
		cw.visitSource(sourceFileName, null);
		fv = cw.visitField(ACC_STATIC, "x", "I", null, new Integer(0));
		fv.visitEnd();
		fv = cw.visitField(ACC_STATIC, "X", "I", null, new Integer(0));
		fv.visitEnd();
		fv = cw.visitField(ACC_STATIC, "y", "I", null, new Integer(0));
		fv.visitEnd();
		fv = cw.visitField(ACC_STATIC, "Y", "I", null, new Integer(0));
		fv.visitEnd();
		fv = cw.visitField(ACC_STATIC, "r", "I", null, new Integer(0));
		fv.visitEnd();
		fv = cw.visitField(ACC_STATIC, "R", "I", null, new Integer(0));
		fv.visitEnd();
		fv = cw.visitField(ACC_STATIC, "a", "I", null, new Integer(0));
		fv.visitEnd();
		fv = cw.visitField(ACC_STATIC, "A", "I", null, new Integer(0));
		fv.visitEnd();
		fv = cw.visitField(ACC_STATIC, "DEF_X", "I", null, new Integer(256));
		fv.visitEnd();
		fv = cw.visitField(ACC_STATIC, "DEF_Y", "I", null, new Integer(256));
		fv.visitEnd();
		fv = cw.visitField(ACC_STATIC, "Z", "I", null, new Integer(16777215));
		fv.visitEnd();
		// create main method
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		// initialize

		mv.visitCode();
		// add label before first instruction
		Label mainStart = new Label();
		mv.visitLabel(mainStart);
		// if GRADE, generates code to add string to log
		// CodeGenUtils.genLog(GRADE, mv, "entering main");

		// visit decs and statements to add field to class
		// and instructions to main method, respectivley
		ArrayList<ASTNode> decsAndStatements = program.decsAndStatements;
		for (ASTNode node : decsAndStatements) {
			node.visit(this, arg);
		}

		// generates code to add string to log
		// CodeGenUtils.genLog(GRADE, mv, "leaving main");

		// adds the required (by the JVM) return statement to main
		mv.visitInsn(RETURN);

		// adds label at end of code
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);

		// handles parameters and local variables of main. Right now, only args
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);

		// Sets max stack size and number of local vars.
		// Because we use ClassWriter.COMPUTE_FRAMES as a parameter in the constructor,
		// asm will calculate this itself and the parameters are ignored.
		// If you have trouble with failures in this routine, it may be useful
		// to temporarily set the parameter in the ClassWriter constructor to 0.
		// The generated classfile will not be correct, but you will at least be
		// able to see what is in it.
		mv.visitMaxs(0, 0);

		// terminate construction of main method
		mv.visitEnd();

		// terminate class construction
		cw.visitEnd();

		// generate classfile as byte array and return
		return cw.toByteArray();
	}

	@Override
	public Object visitDeclaration_Variable(Declaration_Variable declaration_Variable, Object arg) throws Exception {
		Type declarationType = declaration_Variable.getType();
		String name = declaration_Variable.name;
		switch (declarationType) {
		case INTEGER:
			fv = cw.visitField(ACC_STATIC, name, "I", null, new Integer(0));
			break;
		case BOOLEAN:
			fv = cw.visitField(ACC_STATIC, name, "Z", null, false);
			break;
		default:
			throw new UnsupportedOperationException();
		}
		fv.visitEnd();
		Expression e = declaration_Variable.e;
		if (e != null) {
			e.visit(this, arg);
			switch (declarationType) {
			case INTEGER:
				mv.visitFieldInsn(PUTSTATIC, className, name, "I");
				break;
			case BOOLEAN:
				mv.visitFieldInsn(PUTSTATIC, className, name, "Z");
				break;
			default:
				throw new UnsupportedOperationException();
			}
		}
		return null;
	}

	@Override
	public Object visitExpression_Binary(Expression_Binary expression_Binary, Object arg) throws Exception {
		Expression e0 = expression_Binary.e0;
		Label first = null;
		Label second = null;
		if (e0 != null) {
			e0.visit(this, arg);
		}
		Expression e1 = expression_Binary.e1;
		if (e1 != null) {
			e1.visit(this, arg);
		}
		Type e0Type = e0.getType();
		Kind op = expression_Binary.op;
		if (op.equals(Kind.OP_GE) && (e0Type == Type.INTEGER || e0Type == Type.BOOLEAN)) {
			first = new Label();
			second = new Label();
			mv.visitJumpInsn(IF_ICMPLT, first);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(GOTO, second);
			mv.visitLabel(first);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(second);
		} else if (op.equals(Kind.OP_GT) && (e0Type == Type.INTEGER || e0Type == Type.BOOLEAN)) {
			first = new Label();
			second = new Label();
			mv.visitJumpInsn(IF_ICMPLE, first);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(GOTO, second);
			mv.visitLabel(first);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(second);
		} else if (op.equals(Kind.OP_LE) && (e0Type == Type.INTEGER || e0Type == Type.BOOLEAN)) {
			first = new Label();
			second = new Label();
			mv.visitJumpInsn(IF_ICMPGT, first);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(GOTO, second);
			mv.visitLabel(first);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(second);
		} else if (op.equals(Kind.OP_LT) && (e0Type == Type.INTEGER || e0Type == Type.BOOLEAN)) {
			first = new Label();
			second = new Label();
			mv.visitJumpInsn(IF_ICMPGE, first);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(GOTO, second);
			mv.visitLabel(first);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(second);
		} else if (op.equals(Kind.OP_NEQ) && (e0Type == Type.INTEGER || e0Type == Type.BOOLEAN)) {
			first = new Label();
			second = new Label();
			mv.visitJumpInsn(IF_ICMPEQ, first);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(GOTO, second);
			mv.visitLabel(first);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(second);
		} else if (op.equals(Kind.OP_EQ) && (e0Type == Type.INTEGER || e0Type == Type.BOOLEAN)) {
			first = new Label();
			second = new Label();
			mv.visitJumpInsn(IF_ICMPNE, first);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(GOTO, second);
			mv.visitLabel(first);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(second);
		} else if (op.equals(Kind.OP_OR) && (e0Type == Type.INTEGER || e0Type == Type.BOOLEAN)) {
			mv.visitInsn(IOR);
		} else if (op.equals(Kind.OP_AND) && (e0Type == Type.INTEGER || e0Type == Type.BOOLEAN)) {
			mv.visitInsn(IAND);
		} else if (expression_Binary.op == Kind.OP_PLUS && e0Type == Type.INTEGER) {
			mv.visitInsn(IADD);
		} else if (expression_Binary.op == Kind.OP_MINUS && e0Type == Type.INTEGER) {
			mv.visitInsn(ISUB);
		} else if (expression_Binary.op == Kind.OP_TIMES && e0Type == Type.INTEGER) {
			mv.visitInsn(IMUL);
		} else if (expression_Binary.op == Kind.OP_MOD && e0Type == Type.INTEGER) {
			mv.visitInsn(IREM);
		} else if (expression_Binary.op == Kind.OP_DIV && e0Type == Type.INTEGER) {
			mv.visitInsn(IDIV);
		}
		// CodeGenUtils.genLogTOS(GRADE, mv, expression_Binary.getType());
		return null;
	}

	@Override
	public Object visitExpression_Unary(Expression_Unary expression_Unary, Object arg) throws Exception {
		Expression e = expression_Unary.e;
		Kind op = expression_Unary.op;
		Type expressionUnaryType = expression_Unary.getType();
		Label labelTrue = null;
		Label labelFalse = null;
		if (e != null) {
			e.visit(this, arg);
		}
		switch (op) {
		case OP_MINUS:
			mv.visitInsn(INEG);
			break;
		case OP_PLUS:
			break;
		case OP_EXCL:
			if (expressionUnaryType == TypeUtils.Type.BOOLEAN) {
				labelTrue = new Label();
				labelFalse = new Label();
				// mv.visitLdcInsn(new Boolean(false));
				mv.visitJumpInsn(IFEQ, labelFalse);
				mv.visitLdcInsn(0);
				mv.visitJumpInsn(GOTO, labelTrue);
				mv.visitLabel(labelFalse);
				mv.visitLdcInsn(1);
				mv.visitLabel(labelTrue);
			} else if (expressionUnaryType == Type.INTEGER) {
				mv.visitLdcInsn(INTEGER.MAX_VALUE);
				mv.visitInsn(IXOR);
			}
			break;
		default:
			throw new UnsupportedOperationException();
		}
		// CodeGenUtils.genLogTOS(GRADE, mv, expression_Unary.getType());
		return null;
	}

	// generate code to leave the two values on the stack
	@Override
	public Object visitIndex(Index index, Object arg) throws Exception {
		boolean isCartesian = index.isCartesian();
		Expression e0 = index.e0;
		Expression e1 = index.e1;
		e0.visit(this, arg);
		e1.visit(this, arg);
		if (!isCartesian) {
			mv.visitInsn(DUP2);
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "cart_x", RuntimeFunctions.cart_xSig, false);
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "cart_y", RuntimeFunctions.cart_ySig, false);
		}
		return null;
	}

	@Override
	public Object visitExpression_PixelSelector(Expression_PixelSelector expression_PixelSelector, Object arg)
			throws Exception {
		mv.visitFieldInsn(GETSTATIC, className, expression_PixelSelector.name, "Ljava/awt/image/BufferedImage;");
		expression_PixelSelector.index.visit(this, arg);
		mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "getPixel", "(Ljava/awt/image/BufferedImage;II)I",
				false);
		return null;
	}

	@Override
	public Object visitExpression_Conditional(Expression_Conditional expression_Conditional, Object arg)
			throws Exception {
		Expression condition = expression_Conditional.condition;
		Expression falseCondition = expression_Conditional.falseExpression;
		Expression trueCondition = expression_Conditional.trueExpression;
		if (condition != null) {
			condition.visit(this, arg);
		}
		Label labelTrue = new Label();
		Label labelGoto = new Label();
		mv.visitLdcInsn(new Boolean(true));
		mv.visitJumpInsn(IF_ICMPEQ, labelTrue);
		falseCondition.visit(this, arg);
		mv.visitJumpInsn(GOTO, labelGoto);
		mv.visitLabel(labelTrue);
		trueCondition.visit(this, arg);
		mv.visitLabel(labelGoto);
		return null;
	}

	// TODO whether to use parseint and valueof. typecheckvisitor change
	@Override
	public Object visitDeclaration_Image(Declaration_Image declaration_Image, Object arg) throws Exception {
		String name = declaration_Image.name;
		fv = cw.visitField(ACC_STATIC, name, "Ljava/awt/image/BufferedImage;", null, null);
		fv.visitEnd();
		Source src = declaration_Image.source;
		Expression xSize = declaration_Image.xSize;
		Expression ySize = declaration_Image.ySize;
		if (src == null) {
			if (xSize != null && ySize != null) {
				xSize.visit(this, arg);
				ySize.visit(this, arg);
			} else {
				mv.visitFieldInsn(GETSTATIC, className, "DEF_X", "I");
				mv.visitFieldInsn(GETSTATIC, className, "DEF_Y", "I");
			}
			mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "makeImage", ImageSupport.makeImageSig, false);
		} else if (src != null) {
			src.visit(this, arg);
			if (xSize != null && ySize != null) {
				xSize.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
						"(Ljava/lang/String;)Ljava/lang/Integer;", false);
				ySize.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
						"(Ljava/lang/String;)Ljava/lang/Integer;", false);
			} else {
				mv.visitInsn(ACONST_NULL);
				mv.visitInsn(ACONST_NULL);
			}
			mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "readImage", ImageSupport.readImageSig, false);
		}
		mv.visitFieldInsn(PUTSTATIC, className, name, "Ljava/awt/image/BufferedImage;");
		return null;
	}

	@Override
	public Object visitSource_StringLiteral(Source_StringLiteral source_StringLiteral, Object arg) throws Exception {
		mv.visitLdcInsn(source_StringLiteral.fileOrUrl);
		return null;
	}

	@Override
	public Object visitSource_CommandLineParam(Source_CommandLineParam source_CommandLineParam, Object arg)
			throws Exception {
		mv.visitVarInsn(ALOAD, 0);
		source_CommandLineParam.paramNum.visit(this, arg);
		mv.visitInsn(AALOAD);
		return null;
	}

	@Override
	public Object visitSource_Ident(Source_Ident source_Ident, Object arg) throws Exception {
		mv.visitFieldInsn(GETSTATIC, className, source_Ident.name, "Ljava/lang/String;");
		return null;
	}

	@Override
	public Object visitDeclaration_SourceSink(Declaration_SourceSink declaration_SourceSink, Object arg)
			throws Exception {
		String name = declaration_SourceSink.name;
		if (declaration_SourceSink.getType() == Type.IMAGE) {
			fv = cw.visitField(ACC_STATIC, name, "Ljava/awt/image/BufferedImage;", null, null);
		} else {
			fv = cw.visitField(ACC_STATIC, name, "Ljava/lang/String;", null, null);
		}
		fv.visitEnd();
		Source src = declaration_SourceSink.source;
		if (src != null) {
			src.visit(this, arg);
			mv.visitFieldInsn(PUTSTATIC, className, name, "Ljava/lang/String;");
		}
		return null;
	}

	@Override
	public Object visitExpression_IntLit(Expression_IntLit expression_IntLit, Object arg) throws Exception {
		mv.visitLdcInsn(expression_IntLit.value);
		// CodeGenUtils.genLogTOS(GRADE, mv, Type.INTEGER);
		return null;
	}

	@Override
	public Object visitExpression_FunctionAppWithExprArg(
			Expression_FunctionAppWithExprArg expression_FunctionAppWithExprArg, Object arg) throws Exception {
		Expression e = expression_FunctionAppWithExprArg.arg;
		e.visit(this, arg);
		Kind kind = expression_FunctionAppWithExprArg.function;
		switch (kind) {
		case KW_log:
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "log", RuntimeFunctions.logSig, false);
			break;
		case KW_abs:
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "abs", RuntimeFunctions.absSig, false);
			break;
		default:
			throw new UnsupportedOperationException();
		}
		return null;
	}

	@Override
	public Object visitExpression_FunctionAppWithIndexArg(
			Expression_FunctionAppWithIndexArg expression_FunctionAppWithIndexArg, Object arg) throws Exception {
		Index i = expression_FunctionAppWithIndexArg.arg;
		Expression e0 = i.e0;
		Expression e1 = i.e1;
		e0.visit(this, arg);
		e1.visit(this, arg);
		Kind kind = expression_FunctionAppWithIndexArg.function;
		switch (kind) {
		case KW_cart_x:
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "cart_x", "(II)I", false);
			break;
		case KW_cart_y:
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "cart_y", "(II)I", false);
			break;
		case KW_polar_a:
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "polar_a", "(II)I", false);
			break;
		case KW_polar_r:
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "polar_r", "(II)I", false);
			break;
		default:
			throw new UnsupportedOperationException();
		}
		return null;
	}

	@Override
	public Object visitExpression_PredefinedName(Expression_PredefinedName expression_PredefinedName, Object arg)
			throws Exception {
		mv.visitFieldInsn(GETSTATIC, className, expression_PredefinedName.firstToken.getText(), "I");
		return null;
	}

	/**
	 * For Integers and booleans, the only "sink"is the screen, so generate code to
	 * print to console. For Images, load the Image onto the stack and visit the
	 * Sink which will generate the code to handle the image.
	 */
	@Override
	public Object visitStatement_Out(Statement_Out statement_Out, Object arg) throws Exception {
		Type statementType = statement_Out.getDec().getType();
		switch (statementType) {
		case BOOLEAN:
			mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitFieldInsn(GETSTATIC, className, statement_Out.name, "Z");
			CodeGenUtils.genLogTOS(GRADE, mv, Type.BOOLEAN);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Z)V", false);
			break;
		case INTEGER:
			mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitFieldInsn(GETSTATIC, className, statement_Out.name, "I");
			CodeGenUtils.genLogTOS(GRADE, mv, Type.INTEGER);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false);
			break;
		case IMAGE:
			mv.visitFieldInsn(GETSTATIC, className, statement_Out.name, "Ljava/awt/image/BufferedImage;");
			CodeGenUtils.genLogTOS(GRADE, mv, Type.IMAGE);
			statement_Out.sink.visit(this, arg);
			break;
		default:
			throw new UnsupportedOperationException();
		}
		return null;
	}

	/**
	 * Visit source to load rhs, which will be a String, onto the stack
	 * 
	 * In HW5, you only need to handle INTEGER and BOOLEAN Use
	 * java.lang.Integer.parseInt or java.lang.Boolean.parseBoolean to convert
	 * String to actual type.
	 * 
	 * TODO HW6 remaining types
	 */
	@Override
	public Object visitStatement_In(Statement_In statement_In, Object arg) throws Exception {
		Source src = statement_In.source;
		String name = statement_In.name;
		src.visit(this, arg);
		Type statementInType = statement_In.getDec().getType();
		switch (statementInType) {
		case INTEGER:
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
			mv.visitFieldInsn(PUTSTATIC, className, name, "I");
			break;
		case BOOLEAN:
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
			mv.visitFieldInsn(PUTSTATIC, className, name, "Z");
			break;
		case IMAGE:
			Declaration_Image dec = (Declaration_Image) statement_In.getDec();
			if (dec.xSize == null && dec.ySize == null) {
				mv.visitInsn(ACONST_NULL);
				mv.visitInsn(ACONST_NULL);
			} else {
				mv.visitFieldInsn(GETSTATIC, className, name, "Ljava/awt/image/BufferedImage;");
				mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "getX", ImageSupport.getXSig, false);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				mv.visitFieldInsn(GETSTATIC, className, name, "Ljava/awt/image/BufferedImage;");
				mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "getY", ImageSupport.getYSig, false);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
			}
			// mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "readImage",
			// ImageSupport.readImageSig, false);
			// mv.visitFieldInsn(PUTSTATIC, className, name,
			// "Ljava/awt/image/BufferedImage;");
			mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "readImage", ImageSupport.readImageSig, false);
			mv.visitFieldInsn(PUTSTATIC, className, name, "Ljava/awt/image/BufferedImage;");
			break;
		default:
			throw new UnsupportedOperationException();
		}
		return null;
	}

	/**
	 * In HW5, only handle INTEGER and BOOLEAN types.
	 */
	@Override
	public Object visitLHS(LHS lhs, Object arg) throws Exception {
		Type lhsType = lhs.getType();
		if (lhsType == Type.INTEGER) {
			mv.visitFieldInsn(PUTSTATIC, className, lhs.name, "I");
		} else if (lhsType == Type.BOOLEAN) {
			mv.visitFieldInsn(PUTSTATIC, className, lhs.name, "Z");
		} else if (lhsType == Type.IMAGE) {
			mv.visitFieldInsn(GETSTATIC, className, lhs.name, "Ljava/awt/image/BufferedImage;");
			mv.visitFieldInsn(GETSTATIC, className, "x", "I");
			mv.visitFieldInsn(GETSTATIC, className, "y", "I");
			mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "setPixel", ImageSupport.setPixelSig, false);
		}
		return null;
	}

	@Override
	public Object visitSink_SCREEN(Sink_SCREEN sink_SCREEN, Object arg) throws Exception {
		mv.visitMethodInsn(INVOKESTATIC, ImageFrame.className, "makeFrame", ImageSupport.makeFrameSig, false);
		mv.visitInsn(POP);
		return null;
	}

	@Override
	public Object visitSink_Ident(Sink_Ident sink_Ident, Object arg) throws Exception {
		String name = sink_Ident.name;
		mv.visitFieldInsn(GETSTATIC, className, name, "Ljava/lang/String;");
		mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "write", ImageSupport.writeSig, false);
		return null;
	}

	@Override
	public Object visitExpression_BooleanLit(Expression_BooleanLit expression_BooleanLit, Object arg) throws Exception {
		mv.visitLdcInsn(new Boolean(expression_BooleanLit.value));
		// CodeGenUtils.genLogTOS(GRADE, mv, Type.BOOLEAN);
		return null;
	}

	@Override
	public Object visitExpression_Ident(Expression_Ident expression_Ident, Object arg) throws Exception {

		Type identType = expression_Ident.getType();
		switch (identType) {
		case INTEGER:
			mv.visitFieldInsn(GETSTATIC, className, expression_Ident.name, "I");
			break;
		case BOOLEAN:
			mv.visitFieldInsn(GETSTATIC, className, expression_Ident.name, "Z");
			break;
		default:
			throw new UnsupportedOperationException();
		}
		// CodeGenUtils.genLogTOS(GRADE, mv, expression_Ident.getType());
		return null;
	}

	@Override
	public Object visitStatement_Assign(Statement_Assign statement_Assign, Object arg) throws Exception {
		Type type = statement_Assign.lhs.getType();
		String name = statement_Assign.lhs.name;
		switch (type) {
		case BOOLEAN:
		case INTEGER:
			statement_Assign.e.visit(this, arg);
			statement_Assign.lhs.visit(this, arg);
			break;
		case IMAGE:
			mv.visitFieldInsn(GETSTATIC, className, name, "Ljava/awt/image/BufferedImage;");
			mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "getX", ImageSupport.getXSig, false);
			mv.visitFieldInsn(PUTSTATIC, className, "X", "I");
			mv.visitFieldInsn(GETSTATIC, className, name, "Ljava/awt/image/BufferedImage;");
			mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "getY", ImageSupport.getYSig, false);
			mv.visitFieldInsn(PUTSTATIC, className, "Y", "I");
			// x = 0
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(ICONST_0);
			mv.visitFieldInsn(PUTSTATIC, className, "x", "I");
			// check if (x < X)
			Label l3 = new Label();
			mv.visitJumpInsn(GOTO, l3);
			// y = 0
			Label l4 = new Label();
			mv.visitLabel(l4);
			mv.visitInsn(ICONST_0);
			mv.visitFieldInsn(PUTSTATIC, className, "y", "I");
			// check if y < Y
			Label l5 = new Label();
			mv.visitJumpInsn(GOTO, l5);
			// Visit expression and lhs
			Label l6 = new Label();
			mv.visitLabel(l6);
			mv.visitFieldInsn(GETSTATIC, className, "x", "I");
			mv.visitFieldInsn(GETSTATIC, className, "y", "I");
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "polar_r", "(II)I", false);
			mv.visitFieldInsn(PUTSTATIC, className, "r", "I");
			mv.visitFieldInsn(GETSTATIC, className, "x", "I");
			mv.visitFieldInsn(GETSTATIC, className, "y", "I");
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "polar_a", "(II)I", false);
			mv.visitFieldInsn(PUTSTATIC, className, "a", "I");
			statement_Assign.e.visit(this, arg);
			statement_Assign.lhs.visit(this, arg);
			// y++
			Label l7 = new Label();
			mv.visitLabel(l7);
			mv.visitFieldInsn(GETSTATIC, className, "y", "I");
			mv.visitInsn(ICONST_1);
			mv.visitInsn(IADD);
			mv.visitFieldInsn(PUTSTATIC, className, "y", "I");
			// y < Y
			mv.visitLabel(l5);
			mv.visitFieldInsn(GETSTATIC, className, "y", "I");
			mv.visitFieldInsn(GETSTATIC, className, "Y", "I");
			mv.visitJumpInsn(IF_ICMPLT, l6);
			// x++
			Label l8 = new Label();
			mv.visitLabel(l8);
			mv.visitFieldInsn(GETSTATIC, className, "x", "I");
			mv.visitInsn(ICONST_1);
			mv.visitInsn(IADD);
			mv.visitFieldInsn(PUTSTATIC, className, "x", "I");
			// x < X
			mv.visitLabel(l3);
			mv.visitFieldInsn(GETSTATIC, className, "x", "I");
			mv.visitFieldInsn(GETSTATIC, className, "X", "I");
			mv.visitJumpInsn(IF_ICMPLT, l4);
			break;
		default:
			throw new UnsupportedOperationException();
		}

		return null;
	}

}
