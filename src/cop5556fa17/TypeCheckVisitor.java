package cop5556fa17;

import java.net.URL;

import cop5556fa17.Scanner.Kind;
import cop5556fa17.Scanner.Token;
import cop5556fa17.TypeUtils.Type;
import cop5556fa17.AST.ASTNode;
import cop5556fa17.AST.ASTVisitor;
import cop5556fa17.AST.Declaration;
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
import cop5556fa17.AST.Sink;
import cop5556fa17.AST.Sink_Ident;
import cop5556fa17.AST.Sink_SCREEN;
import cop5556fa17.AST.Source;
import cop5556fa17.AST.Source_CommandLineParam;
import cop5556fa17.AST.Source_Ident;
import cop5556fa17.AST.Source_StringLiteral;
import cop5556fa17.AST.Statement_Assign;
import cop5556fa17.AST.Statement_In;
import cop5556fa17.AST.Statement_Out;

public class TypeCheckVisitor implements ASTVisitor {

	public SymbolTable symbolTable;

	TypeCheckVisitor() {
		symbolTable = new SymbolTable();
	}

	@SuppressWarnings("serial")
	public static class SemanticException extends Exception {
		Token t;

		public SemanticException(Token t, String message) {
			super("line " + t.line + " pos " + t.pos_in_line + ": " + message);
			this.t = t;
		}

	}

	/**
	 * The program name is only used for naming the class. It does not rule out
	 * variables with the same name. It is returned for convenience.
	 * 
	 * @throws Exception
	 */

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		for (ASTNode node : program.decsAndStatements) {
			node.visit(this, arg);
		}
		return program.name;
	}

	@Override
	public Object visitDeclaration_Variable(Declaration_Variable declaration_Variable, Object arg) throws Exception {
		Expression e = declaration_Variable.e;
		if (e != null) {
			e.visit(this, arg);
		}
		String name = declaration_Variable.name;
		verifyNull(name, declaration_Variable.firstToken);
		symbolTable.insert(name, declaration_Variable);
		declaration_Variable.setType(TypeUtils.getType(declaration_Variable.type));
		if (e != null) {
			verifySameType(declaration_Variable.getType(), e.getType(), declaration_Variable);
		}
		return arg;
	}

	@Override
	public Object visitExpression_Binary(Expression_Binary expression_Binary, Object arg) throws Exception {
		Expression e0 = expression_Binary.e0;
		e0.visit(this, arg);
		Expression e1 = expression_Binary.e1;
		if (e1 != null) {
			e1.visit(this, arg);
			verifySameType(e0.getType(), e1.getType(), expression_Binary);
		}
		Kind op = expression_Binary.op;
		if (op.equals(Kind.OP_EQ) || op.equals(Kind.OP_NEQ)) {
			expression_Binary.setType(Type.BOOLEAN);
		} else if (op.equals(Kind.OP_GE) || op.equals(Kind.OP_GT) || op.equals(Kind.OP_LE) || op.equals(Kind.OP_LT)) {
			if (e0.getType() == Type.INTEGER) {
				expression_Binary.setType(Type.BOOLEAN);
			}
		} else if (op.equals(Kind.OP_AND) || op.equals(Kind.OP_OR)) {
			if (e0.getType() == Type.INTEGER || e0.getType() == Type.BOOLEAN) {
				expression_Binary.setType(e0.getType());
			}
		} else if (op.equals(Kind.OP_DIV) || op.equals(Kind.OP_MINUS) || op.equals(Kind.OP_MOD)
				|| op.equals(Kind.OP_PLUS) || op.equals(Kind.OP_POWER) || op.equals(Kind.OP_TIMES)) {
			if (e0.getType() == Type.INTEGER) {
				expression_Binary.setType(Type.INTEGER);
			}
		}
		if (expression_Binary.getType() == null) {
			throw new SemanticException(expression_Binary.firstToken, "Expression Binary type should not be null");
		}
		return arg;
	}

	@Override
	public Object visitExpression_Unary(Expression_Unary expression_Unary, Object arg) throws Exception {
		Expression expression = expression_Unary.e;
		expression.visit(this, arg);
		Type t = expression.getType();
		Kind op = expression_Unary.op;
		if (op.equals(Kind.OP_EXCL) && (t == Type.BOOLEAN || t == Type.INTEGER)) {
			expression_Unary.setType(t);
		} else if ((op.equals(Kind.OP_PLUS) || op.equals(Kind.OP_MINUS)) && t == Type.INTEGER) {
			expression_Unary.setType(Type.INTEGER);
		}
		if (expression_Unary.getType() == null) {
			throw new SemanticException(expression_Unary.firstToken, "Expression Unary type should not be null");
		}
		return arg;
	}

	@Override
	public Object visitIndex(Index index, Object arg) throws Exception {
		Expression e0 = index.e0;
		Expression e1 = index.e1;
		e0.visit(this, arg);
		e1.visit(this, arg);
		verifySameType(e0.getType(), Type.INTEGER, e0);
		verifySameType(e1.getType(), Type.INTEGER, e1);
		if (e0 instanceof Expression_PredefinedName && e1 instanceof Expression_PredefinedName) {
			Expression_PredefinedName ep0 = (Expression_PredefinedName) e0;
			Expression_PredefinedName ep1 = (Expression_PredefinedName) e1;
			Kind e0Kind = ep0.kind;
			Kind e1Kind = ep1.kind;
			index.setCartesian(!(e0Kind.equals(Kind.KW_r) && e1Kind.equals(Kind.KW_a)));
		}
		return arg;
	}

	@Override
	public Object visitExpression_PixelSelector(Expression_PixelSelector expression_PixelSelector, Object arg)
			throws Exception {
		String name = expression_PixelSelector.name;
		Declaration dec = symbolTable.lookupDec(name);
		verifyNotNull(dec, expression_PixelSelector.firstToken);
		Type nameType = dec.getType();
		Index index = expression_PixelSelector.index;
		if (index != null) {
			index.visit(this, arg);
		}
		if (nameType == Type.IMAGE) {
			expression_PixelSelector.setType(Type.INTEGER);
		} else if (index == null) {
			expression_PixelSelector.setType(nameType);
		}
		if (expression_PixelSelector.getType() == null) {
			throw new SemanticException(expression_PixelSelector.firstToken,
					"Expression PixelSelector type should not be null");
		}
		return arg;
	}

	@Override
	public Object visitExpression_Conditional(Expression_Conditional expression_Conditional, Object arg)
			throws Exception {
		Expression condition = expression_Conditional.condition;
		Expression trueExpression = expression_Conditional.trueExpression;
		Expression falseExpression = expression_Conditional.falseExpression;
		condition.visit(this, arg);
		trueExpression.visit(this, arg);
		falseExpression.visit(this, arg);
		verifySameType(condition.getType(), Type.BOOLEAN, condition);
		verifySameType(trueExpression.getType(), falseExpression.getType(), expression_Conditional);
		expression_Conditional.setType(trueExpression.getType());
		return arg;
	}

	@Override
	public Object visitDeclaration_Image(Declaration_Image declaration_Image, Object arg) throws Exception {
		Source src = declaration_Image.source;
		if (src != null) {
			src.visit(this, arg);
		}
		Expression xSize = declaration_Image.xSize;
		Expression ySize = declaration_Image.ySize;
		if (xSize != null) {
			if (ySize == null) {
				throw new SemanticException(xSize.firstToken, "ySize should not be null");
			}
			xSize.visit(this, arg);
			ySize.visit(this, arg);
			verifySameType(xSize.getType(), Type.INTEGER, xSize);
			verifySameType(ySize.getType(), Type.INTEGER, ySize);
		}
		String name = declaration_Image.name;
		verifyNull(name, declaration_Image.firstToken);
		symbolTable.insert(name, declaration_Image);
		declaration_Image.setType(Type.IMAGE);
		return arg;
	}

	@Override
	public Object visitSource_StringLiteral(Source_StringLiteral source_StringLiteral, Object arg) throws Exception {
		try {
			new URL(source_StringLiteral.fileOrUrl);
			source_StringLiteral.setType(Type.URL);
		} catch (Exception e) {
			source_StringLiteral.setType(Type.FILE);
		}
		return arg;
	}

	@Override
	public Object visitSource_CommandLineParam(Source_CommandLineParam source_CommandLineParam, Object arg)
			throws Exception {
		Expression paramNum = source_CommandLineParam.paramNum;
		paramNum.visit(this, arg);
		// source_CommandLineParam.setType(paramNum.getType());
		source_CommandLineParam.setType(null);
		// verifySameType(source_CommandLineParam.getType(), Type.INTEGER,
		// source_CommandLineParam);
		verifySameType(paramNum.getType(), Type.INTEGER, paramNum);
		return arg;
	}

	@Override
	public Object visitSource_Ident(Source_Ident source_Ident, Object arg) throws Exception {
		String name = source_Ident.name;
		Declaration dec = symbolTable.lookupDec(name);
		verifyNotNull(dec, source_Ident.firstToken);
		source_Ident.setType(dec.getType());
		if (!(source_Ident.getType() == Type.FILE || source_Ident.getType() == Type.URL)) {
			throw new SemanticException(source_Ident.firstToken, "Type should be file or url");
		}
		return arg;
	}

	@Override
	public Object visitDeclaration_SourceSink(Declaration_SourceSink declaration_SourceSink, Object arg)
			throws Exception {
		Source src = declaration_SourceSink.source;
		src.visit(this, arg);
		String name = declaration_SourceSink.name;
		verifyNull(name, declaration_SourceSink.firstToken);
		symbolTable.insert(name, declaration_SourceSink);
		declaration_SourceSink.setType(TypeUtils.getType(declaration_SourceSink.firstToken));
		if (!(src.getType() == null || src.getType() == declaration_SourceSink.getType())) {
			throw new SemanticException(declaration_SourceSink.firstToken, "Error in dec_sink");
		}
		// verifySameType(src.getType(), declaration_SourceSink.getType(),
		// declaration_SourceSink);
		return arg;
	}

	@Override
	public Object visitExpression_IntLit(Expression_IntLit expression_IntLit, Object arg) throws Exception {
		expression_IntLit.setType(Type.INTEGER);
		return arg;
	}

	@Override
	public Object visitExpression_FunctionAppWithExprArg(
			Expression_FunctionAppWithExprArg expression_FunctionAppWithExprArg, Object arg) throws Exception {
		Expression e = expression_FunctionAppWithExprArg.arg;
		e.visit(this, arg);
		verifySameType(e.getType(), Type.INTEGER, e);
		expression_FunctionAppWithExprArg.setType(Type.INTEGER);
		return arg;
	}

	@Override
	public Object visitExpression_FunctionAppWithIndexArg(
			Expression_FunctionAppWithIndexArg expression_FunctionAppWithIndexArg, Object arg) throws Exception {
		Index i = expression_FunctionAppWithIndexArg.arg;
		i.visit(this, arg);
		expression_FunctionAppWithIndexArg.setType(Type.INTEGER);
		return arg;
	}

	@Override
	public Object visitExpression_PredefinedName(Expression_PredefinedName expression_PredefinedName, Object arg)
			throws Exception {
		expression_PredefinedName.setType(Type.INTEGER);
		return arg;
	}

	@Override
	public Object visitStatement_Out(Statement_Out statement_Out, Object arg) throws Exception {
		Sink sink = statement_Out.sink;
		sink.visit(this, arg);
		String name = statement_Out.name;
		Declaration dec = symbolTable.lookupDec(name);
		verifyNotNull(dec, statement_Out.firstToken);
		statement_Out.setDec(dec);
		Type nameType = symbolTable.lookupType(name);
		Type sinkType = sink.getType();
		if (nameType == Type.INTEGER || nameType == Type.BOOLEAN) {
			verifySameType(sinkType, Type.SCREEN, sink);
		} else if (nameType == Type.IMAGE) {
			if (!(sinkType == Type.FILE || sinkType == Type.SCREEN)) {
				throw new SemanticException(sink.firstToken, "Sink type should be file or screen");
			}
		} else
			throw new SemanticException(statement_Out.firstToken, "Type of name should be int, bool or image");
		return arg;

	}

	@Override
	public Object visitStatement_In(Statement_In statement_In, Object arg) throws Exception {
		Source source = statement_In.source;
		if (source != null) {
			source.visit(this, arg);
		}
		String name = statement_In.name;
		Declaration dec = symbolTable.lookupDec(name);
		verifyNotNull(dec, statement_In.firstToken);
		statement_In.setDec(dec);
		return arg;
	}

	@Override
	public Object visitStatement_Assign(Statement_Assign statement_Assign, Object arg) throws Exception {
		LHS lhs = statement_Assign.lhs;
		lhs.visit(this, arg);
		Expression e = statement_Assign.e;
		e.visit(this, arg);
		TypeUtils.Type lhsType = lhs.getType();
		TypeUtils.Type expressionType = e.getType();
		if (lhs.getType() != Type.IMAGE) {
			verifySameType(lhsType, expressionType, statement_Assign);
		} else {
			verifySameType(expressionType, Type.INTEGER, statement_Assign);
		}
		statement_Assign.setCartesian(lhs.isCartesian());
		return arg;
	}

	@Override
	public Object visitLHS(LHS lhs, Object arg) throws Exception {
		String name = lhs.name;
		Declaration lhsDeclaration = symbolTable.lookupDec(name);
		verifyNotNull(lhsDeclaration, lhs.firstToken);
		lhs.setDec(lhsDeclaration);
		Type lhsDeclarationType = lhsDeclaration.getType();
		lhs.setType(lhsDeclarationType);
		Index lhsIndex = lhs.index;
		if (lhsIndex != null) {
			lhsIndex.visit(this, arg);
			lhs.setCartesian(lhsIndex.isCartesian());
		}
		return arg;

	}

	@Override
	public Object visitSink_SCREEN(Sink_SCREEN sink_SCREEN, Object arg) throws Exception {
		sink_SCREEN.setType(Type.SCREEN);
		return arg;
	}

	@Override
	public Object visitSink_Ident(Sink_Ident sink_Ident, Object arg) throws Exception {
		String name = sink_Ident.name;
		Declaration dec = symbolTable.lookupDec(name);
		verifyNotNull(dec, sink_Ident.firstToken);
		sink_Ident.setType(dec.getType());
		verifySameType(sink_Ident.getType(), Type.FILE, sink_Ident);
		return arg;
	}

	@Override
	public Object visitExpression_BooleanLit(Expression_BooleanLit expression_BooleanLit, Object arg) throws Exception {
		expression_BooleanLit.setType(Type.BOOLEAN);
		return arg;
	}

	@Override
	public Object visitExpression_Ident(Expression_Ident expression_Ident, Object arg) throws Exception {
		String name = expression_Ident.name;
		Declaration dec = symbolTable.lookupDec(name);
		verifyNotNull(dec, expression_Ident.firstToken);
		expression_Ident.setType(dec.getType());
		return arg;
	}

	private void verifyNull(String name, Token t) throws SemanticException {
		if (symbolTable.lookupDec(name) != null) {
			throw new SemanticException(t, "Entry for " + name + " should be null");
		}
	}

	private void verifyNotNull(Declaration dec, Token t) throws SemanticException {
		if (dec == null) {
			throw new SemanticException(t, "Dec should not be null");
		}
	}

	private void verifySameType(TypeUtils.Type first, TypeUtils.Type second, ASTNode node) throws SemanticException {
		String message = "Type of first token = " + first + " and second token is " + second;
		message += "Types should be same!";
		if (first != second) {
			throw new SemanticException(node.firstToken, message);
		}
	}

}
