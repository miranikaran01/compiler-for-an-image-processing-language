package cop5556fa17;

import static cop5556fa17.Scanner.Kind.EOF;

import java.util.ArrayList;

import cop5556fa17.Scanner.Kind;
import cop5556fa17.Scanner.Token;
import cop5556fa17.AST.ASTNode;
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
import cop5556fa17.AST.Statement;
import cop5556fa17.AST.Statement_Assign;
import cop5556fa17.AST.Statement_In;
import cop5556fa17.AST.Statement_Out;

public class Parser {
	ArrayList<ASTNode> decsAndStatements;
	Scanner scanner;
	Token t;

	@SuppressWarnings("serial")
	public class SyntaxException extends Exception {
		Token t;

		public SyntaxException(Token t, String message) {
			super(message);
			this.t = t;
		}

	}

	Parser(Scanner scanner) {
		this.scanner = scanner;
		t = scanner.nextToken();
		decsAndStatements = new ArrayList<>();
	}

	public Program parse() throws SyntaxException {
		Program p = program();
		matchEOF();
		return p;
	}

	@SuppressWarnings("incomplete-switch")
	Program program() throws SyntaxException {

		Token firstToken = match(Kind.IDENTIFIER);
		while (t.kind == Kind.KW_int || t.kind == Kind.KW_boolean || t.kind == Kind.KW_image || t.kind == Kind.KW_file
				|| t.kind == Kind.IDENTIFIER || t.kind == Kind.KW_url) {
			switch (t.kind) {
			case KW_int:
			case KW_boolean:
				decsAndStatements.add(variableDeclaration());
				break;
			case KW_image:
				decsAndStatements.add(imageDeclaration());
				break;
			case KW_url:
			case KW_file:
				decsAndStatements.add(sourceSinkDeclaration());
				break;
			case IDENTIFIER:
				decsAndStatements.add(statement());
				break;
			}
			match(Kind.SEMI);
		}
		return new Program(firstToken, firstToken, decsAndStatements);

	}

	Statement statement() throws SyntaxException {
		Token firstToken = consume();
		switch (t.kind) {
		case OP_RARROW:
			return imageOutStatement(firstToken);
		case OP_LARROW:
			return imageInStatement(firstToken);
		default:
			LHS l = lhs(firstToken);
			match(Kind.OP_ASSIGN);
			Expression e = expression();
			return new Statement_Assign(firstToken, l, e);
		}
	}

	Statement_In imageInStatement(Token firstToken) throws SyntaxException {
		consume();
		Source src = source();
		return new Statement_In(firstToken, firstToken, src);
	}

	Statement_Out imageOutStatement(Token firstToken) throws SyntaxException {
		consume();
		Sink s = sink();
		return new Statement_Out(firstToken, firstToken, s);
	}

	LHS lhs(Token firstToken) throws SyntaxException {
		Index i = null;
		if (t.kind == Kind.LSQUARE) {
			consume();
			match(Kind.LSQUARE);
			if (t.kind == Kind.KW_x) {
				i = xySelector();
			} else {
				i = raSelector();
			}
			match(Kind.RSQUARE);
			match(Kind.RSQUARE); // TODO changed this
		}
		return new LHS(firstToken, firstToken, i);
	}

	Declaration_SourceSink sourceSinkDeclaration() throws SyntaxException {
		Token firstToken = consume();
		Token name = match(Kind.IDENTIFIER);
		match(Kind.OP_ASSIGN);
		Source s = source();
		return new Declaration_SourceSink(firstToken, firstToken, name, s);
	}

	Declaration_Image imageDeclaration() throws SyntaxException {
		Token firstToken = consume();
		Expression ex = null;
		Expression ey = null;
		Source s = null;
		if (t.kind == Kind.LSQUARE) {
			consume();
			ex = expression();
			match(Kind.COMMA);
			ey = expression();
			match(Kind.RSQUARE);
		}
		Token name = match(Kind.IDENTIFIER);
		if (t.kind == Kind.OP_LARROW) {
			consume();
			s = source();
		}
		return new Declaration_Image(firstToken, ex, ey, name, s);
	}

	Declaration_Variable variableDeclaration() throws SyntaxException {
		Token firstToken = consume();
		Token name = match(Kind.IDENTIFIER);
		Expression e = null; // assuming expression points to the next token to be consumed
		if (t.kind == Kind.OP_ASSIGN) {
			consume();
			e = expression();
		}
		return new Declaration_Variable(firstToken, firstToken, name, e);
	}

	Source source() throws SyntaxException {
		Token firstToken = t;
		switch (t.kind) {
		case STRING_LITERAL:
			consume();
			return new Source_StringLiteral(firstToken, firstToken.getText());
		case IDENTIFIER:
			Token name = consume();
			return new Source_Ident(firstToken, name);
		default:
			match(Kind.OP_AT);
			Expression e = expression();
			return new Source_CommandLineParam(firstToken, e);
		}
	}

	Expression orExpression() throws SyntaxException {
		Token firstToken = t;
		Expression e0 = andExpression();
		Token op = null;
		Expression e1 = null;
		while (t.kind == Kind.OP_OR) {
			op = consume();
			e1 = andExpression();
			e0 = new Expression_Binary(firstToken, e0, op, e1);
		}
		return e0;
	}

	Expression andExpression() throws SyntaxException {
		Token firstToken = t;
		Expression e0 = eqExpression();
		Token op = null;
		Expression e1 = null;
		while (t.kind == Kind.OP_AND) {
			op = consume();
			e1 = eqExpression();
			e0 = new Expression_Binary(firstToken, e0, op, e1);
		}
		return e0;

	}

	Expression eqExpression() throws SyntaxException {
		Token firstToken = t;
		Expression e0 = relExpression();
		Token op = null;
		Expression e1 = null;
		while (t.kind == Kind.OP_EQ || t.kind == Kind.OP_NEQ) {
			op = consume();
			e1 = relExpression();
			e0 = new Expression_Binary(firstToken, e0, op, e1);
		}
		return e0;
	}

	Expression relExpression() throws SyntaxException {
		Token firstToken = t;
		Expression e0 = addExpression();
		Token op = null;
		Expression e1 = null;
		while (t.kind == Kind.OP_LT || t.kind == Kind.OP_GT || t.kind == Kind.OP_LE || t.kind == Kind.OP_GE) {
			op = consume();
			e1 = addExpression();
			e0 = new Expression_Binary(firstToken, e0, op, e1);
		}
		return e0;
	}

	Expression addExpression() throws SyntaxException {
		Token firstToken = t;
		Expression e0 = multExpression();
		Token op = null;
		Expression e1 = null;
		while (t.kind == Kind.OP_PLUS || t.kind == Kind.OP_MINUS) {
			op = consume();
			e1 = multExpression();
			e0 = new Expression_Binary(firstToken, e0, op, e1);
		}
		return e0;

	}

	Expression multExpression() throws SyntaxException {
		Token firstToken = t;
		Expression eb = unaryExpression();
		Token op = null;
		Expression e1 = null;
		while (t.kind == Kind.OP_TIMES || t.kind == Kind.OP_DIV || t.kind == Kind.OP_MOD) {
			op = consume();
			e1 = unaryExpression();
			eb = new Expression_Binary(firstToken, eb, op, e1);
		}
		return eb;
	}

	Expression unaryExpression() throws SyntaxException {
		Token firstToken = t;
		Expression e = null;
		switch (t.kind) {
		case OP_PLUS:
		case OP_MINUS:
		case OP_EXCL:
			Token op = consume();
			e = unaryExpression();
			e = new Expression_Unary(firstToken, op, e);
			break;
		default:
			e = unaryExpressionNotPlusMinus();
		}
		return e;
	}

	Expression unaryExpressionNotPlusMinus() throws SyntaxException {
		Token firstToken = t;
		switch (t.kind) {
		case IDENTIFIER:
			return identOrPixelSelectorExpression();
		case KW_x:
		case KW_X:
		case KW_y:
		case KW_Y:
		case KW_r:
		case KW_R:
		case KW_a:
		case KW_A:
		case KW_Z:
		case KW_DEF_X:
		case KW_DEF_Y:
			return predefinedName();
		case INTEGER_LITERAL:
			consume();
			return new Expression_IntLit(firstToken, firstToken.intVal());
		case BOOLEAN_LITERAL:
			consume();
			return new Expression_BooleanLit(firstToken, firstToken.getText().equals("true"));
		// Function Application
		case KW_sin:
		case KW_cos:
		case KW_atan:
		case KW_abs:
		case KW_cart_x:
		case KW_cart_y:
		case KW_polar_a:
		case KW_polar_r:
			consume();
			if (t.kind == Kind.LPAREN) {
				consume();
				Expression e = expression();
				match(Kind.RPAREN);
				return new Expression_FunctionAppWithExprArg(firstToken, firstToken.kind, e);
			} else {
				match(Kind.LSQUARE);
				Index i = selector();
				match(Kind.RSQUARE);
				return new Expression_FunctionAppWithIndexArg(firstToken, firstToken.kind, i);
			}
			// Primary LPAREN
		default:
			match(Kind.LPAREN);
			Expression e = expression();
			match(Kind.RPAREN);
			return e;
		}
	}

	Expression_PredefinedName predefinedName() throws SyntaxException {
		Token firstToken = consume();
		return new Expression_PredefinedName(firstToken, firstToken.kind);
	}

	Expression identOrPixelSelectorExpression() throws SyntaxException {
		Token firstToken = consume();
		Index i = null;
		if (t.kind == Kind.LSQUARE) {
			consume();
			i = selector();
			match(Kind.RSQUARE);
			return new Expression_PixelSelector(firstToken, firstToken, i);
		}
		return new Expression_Ident(firstToken, firstToken);
	}

	Index selector() throws SyntaxException {
		Token firstToken = t;
		Expression e0 = expression();
		match(Kind.COMMA);
		Expression e1 = expression();
		return new Index(firstToken, e0, e1);
	}

	Sink sink() throws SyntaxException {
		Token firstToken = t;
		if (t.kind == Kind.IDENTIFIER) {
			consume();
			return new Sink_Ident(firstToken, firstToken);
		} else {
			match(Kind.KW_SCREEN);
			return new Sink_SCREEN(firstToken);
		}
	}

	Index raSelector() throws SyntaxException {
		Token firstToken = t;
		// Token firstToken = match(Kind.KW_r);
		Expression e0 = null;
		Expression e1 = null;
		if (t.kind == Kind.KW_r) {
			e0 = expression();
		}
		match(Kind.COMMA);
		if (t.kind == Kind.KW_a) {
			e1 = expression();
		}
		// match(Kind.KW_a);
		return new Index(firstToken, e0, e1);
	}

	Index xySelector() throws SyntaxException {
		Token firstToken = t;
		Expression e0 = expression();
		match(Kind.COMMA);
		Expression e1 = null;
		if (t.kind == Kind.KW_y) {
			e1 = expression();
		} else
			match(Kind.KW_y);
		// match(Kind.KW_y);
		return new Index(firstToken, e0, e1);
	}

	Expression expression() throws SyntaxException {
		Token firstToken = t;
		Expression condition = orExpression(); // consume Token in orExpression
		Expression trueExpression = null;
		Expression falseExpression = null;
		if (t.kind == Kind.OP_Q) {
			consume();
			trueExpression = expression();
			match(Kind.OP_COLON);
			falseExpression = expression();
			condition = new Expression_Conditional(firstToken, condition, trueExpression, falseExpression);
		}
		return condition;
	}

	/**
	 * Only for check at end of program. Does not "consume" EOF so no attempt to get
	 * nonexistent next Token.
	 * 
	 * @return
	 * @throws SyntaxException
	 */

	private Token consume() throws SyntaxException {
		Token prev = t;
		t = scanner.nextToken();
		return prev;
	}

	private Token match(Kind kind) throws SyntaxException {
		if (t.kind == kind) {
			return consume();
		}
		throw new SyntaxException(t, "Token found on line " + t.line + " at position " + t.pos_in_line + ": "
				+ t.toString() + "\nExpected token to be of kind: " + kind);
	}

	private Token matchEOF() throws SyntaxException {
		if (t.kind == EOF) {
			return t;
		}
		String message = "Expected EOL at " + t.line + ":" + t.pos_in_line;
		throw new SyntaxException(t, message);
	}
}
