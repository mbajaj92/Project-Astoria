package Utilities;

import Utilities.Utils.RESULT_KIND;

public class Grammer {
	private static MyScanner sc;

	private static int relOp() throws Exception {
		int code = -1;
		switch (sc.currentToken) {
		case ScannerUtils.leqToken:
		case ScannerUtils.neqToken:
		case ScannerUtils.eqlToken:
		case ScannerUtils.geqToken:
		case ScannerUtils.gtrToken:
		case ScannerUtils.lssToken:
			code = sc.currentToken;
			sc.next();
			break;
		default:
			Utils.error("Expected Relaional Operator Token, got " + sc.token + " at line " + sc.linecount);
		}
		return code;
	}

	private static Result relation() throws Exception {
		Result X = expression();
		int code = relOp();
		Result Y = expression();
		Utils.compute(code, X, Y);
		return X;
	}

	private static Result expression() throws Exception {
		Result X = term();
		while (sc.currentToken == ScannerUtils.plusToken || sc.currentToken == ScannerUtils.minusToken) {
			int code = sc.currentToken;
			sc.next();
			Result Y = term();
			Utils.compute(code, X, Y);
		}
		return X;
	}

	private static Result term() throws Exception {
		Result X = factor();
		while (sc.currentToken == ScannerUtils.timesToken || sc.currentToken == ScannerUtils.divToken) {
			int code = sc.currentToken;
			sc.next();
			Result Y = factor();
			Utils.compute(code, X, Y);
		}
		return X;
	}

	private static Result factor() throws Exception {
		Result X = new Result();
		switch (sc.currentToken) {
		case ScannerUtils.ident:
			X = designator();
			break;
		case ScannerUtils.number:
			X = number();
			break;
		case ScannerUtils.openparanToken:
			sc.next();
			X = expression();
			if (sc.currentToken != ScannerUtils.closeparanToken)
				Utils.error("Expected ) token, got " + sc.token + " at line " + sc.linecount);
			sc.next();
			break;
		case ScannerUtils.callToken:
			funcCall();
			break;
		}
		return X;
	}

	private static Result designator() throws Exception {
		Result X = ident();
		while (sc.currentToken == ScannerUtils.openbracketToken) {
			sc.next();
			expression();
			if (sc.currentToken != ScannerUtils.closebracketToken)
				Utils.error("Expected token ], got " + sc.token + " at line " + sc.linecount);
			sc.next();
		}
		return X;
	}

	private static void returnStatement() throws Exception {
		if (sc.currentToken != ScannerUtils.returnToken)
			Utils.error("Expected return token, got " + sc.token + " at line " + sc.linecount);
		sc.next();

		expression();
	}

	private static void whileStatement() throws Exception {
		if (sc.currentToken != ScannerUtils.whileToken)
			Utils.error("Expected while token, got " + sc.token + " at line " + sc.linecount);
		sc.next();

		relation();
		// basic block ends
		// new basic block starts
		if (sc.currentToken == ScannerUtils.doToken) {
			sc.next();
			statSequence();
			// basic block ends
			// new basic block starts
			if (sc.currentToken != ScannerUtils.odToken)
				Utils.error("Expected od token, got " + sc.token + " at line " + sc.linecount);
			sc.next();
		} else
			Utils.error("Expected do token, got " + sc.token + " at line " + sc.linecount);
	}

	private static void ifStatement() throws Exception {
		if (sc.currentToken != ScannerUtils.ifToken)
			Utils.error("Expected If token, got " + sc.token + " at line " + sc.linecount);
		sc.next();

		relation();
		// new basic block
		if (sc.currentToken == ScannerUtils.thenToken) {
			sc.next();
			statSequence();
			// basic block ends
			// new basic block
			if (sc.currentToken == ScannerUtils.elseToken) {
				sc.next();
				statSequence();
			}
			// basic block ends
			// new basic block
			if (sc.currentToken == ScannerUtils.fiToken) {
				sc.next();
			} else
				Utils.error("Expected fi token, got " + sc.token + " at line " + sc.linecount);
		} else
			Utils.error("Expected then token, got " + sc.token + " at line " + sc.linecount);
	}

	private static void funcCall() throws Exception {
		if (sc.currentToken != ScannerUtils.callToken)
			Utils.error("Expected call token, got " + sc.token + " at line " + sc.linecount);
		sc.next();

		ident();
		if (sc.currentToken == ScannerUtils.openparanToken) {
			sc.next();
			expression();
			while (sc.currentToken == ScannerUtils.commaToken) {
				sc.next();
				expression();
			}

			if (sc.currentToken == ScannerUtils.closeparanToken) {
				sc.next();
			} else
				Utils.error("Expected ), got " + sc.token + " at line " + sc.linecount);
		}
	}

	private static void assignment() throws Exception {
		if (sc.currentToken != ScannerUtils.letToken)
			Utils.error("Expected let token, got " + sc.token + " at line " + sc.linecount);
		sc.next();
		Result X = designator();
		if (sc.currentToken == ScannerUtils.becomesToken) {
			int code = ScannerUtils.becomesToken;
			sc.next();
			Result Y = expression();
			Utils.compute(code, X, Y);
		} else
			Utils.error("Expected <-, got " + sc.token + " at line " + sc.linecount);
	}

	private static void statement() throws Exception {
		switch (sc.currentToken) {
		case ScannerUtils.letToken:
			assignment();
			break;
		case ScannerUtils.callToken:
			funcCall();
			break;
		case ScannerUtils.ifToken:
			ifStatement();
			break;
		case ScannerUtils.whileToken:
			whileStatement();
			break;
		case ScannerUtils.returnToken:
			returnStatement();
			break;
		}
	}

	private static void statSequence() throws Exception {
		statement();

		while (sc.currentToken == ScannerUtils.semiToken) {
			sc.next();
			statement();
		}
	}

	private static void funcBody() throws Exception {
		while (varDecl())
			;
		if (sc.currentToken == ScannerUtils.beginToken) {
			sc.next();
			statSequence();

			if (sc.currentToken != ScannerUtils.endToken)
				Utils.error("Expected } token, but got " + sc.token + " at line " + sc.linecount);
			sc.next();
		} else
			Utils.error("Expected { token, but got " + sc.token + " at line " + sc.linecount);
	}

	private static void formalParam() throws Exception {
		if (sc.currentToken == ScannerUtils.openparanToken) {
			sc.next();

			if (sc.currentToken == ScannerUtils.ident) {
				sc.next();
				while (sc.currentToken == ScannerUtils.commaToken) {
					sc.next();
					ident();
				}
			}

			if (sc.currentToken != ScannerUtils.closeparanToken)
				Utils.error("Expected ), but found " + sc.token + " at line " + sc.linecount);
			sc.next();
		}
	}

	private static boolean funcDecl() throws Exception {
		if (sc.currentToken == ScannerUtils.funcToken || sc.currentToken == ScannerUtils.procToken) {
			sc.next();

			ident();
			formalParam();

			if (sc.currentToken == ScannerUtils.semiToken) {
				sc.next();
				funcBody();
				if (!(sc.currentToken == ScannerUtils.semiToken))
					Utils.error("Expected ; but found " + sc.token);
				sc.next();
			} else
				Utils.error("Expected ; but found " + sc.token);
			return true;
		}
		return false;
	}

	private static Result ident() throws Exception {
		if (sc.currentToken != ScannerUtils.ident)
			Utils.error("Expected Identifier, got " + sc.token + " at line " + sc.linecount);
		Result X = new Result();
		X.kind = RESULT_KIND.VAR;
		X.address = sc.id;
		sc.next();
		return X;
	}

	private static Result number() throws Exception {
		if (sc.currentToken != ScannerUtils.number)
			Utils.error("Expected number, got " + sc.token + " at line " + sc.linecount);
		Result X = new Result();
		X.kind = RESULT_KIND.CONST;
		X.value = sc.val;
		sc.next();
		return X;
	}

	private static boolean typeDecl() throws Exception {
		if (sc.currentToken != ScannerUtils.varToken && sc.currentToken != ScannerUtils.arrToken)
			return false;

		if (sc.currentToken == ScannerUtils.varToken) {
			sc.next();
		} else if (sc.currentToken == ScannerUtils.arrToken) {
			sc.next();
			if (sc.currentToken == ScannerUtils.openbracketToken) {
				sc.next();
				number();
				if (sc.currentToken == ScannerUtils.closebracketToken) {
					sc.next();
				} else
					Utils.error("Expected ], but got " + sc.token + " at line " + sc.linecount);

				while (sc.currentToken == ScannerUtils.openbracketToken) {
					sc.next();
					number();
					if (sc.currentToken == ScannerUtils.closebracketToken) {
						sc.next();
					} else
						Utils.error("Expected ], but got " + sc.token + " at line " + sc.linecount);
				}
			} else
				Utils.error("Expected [, but got " + sc.token + " at line " + sc.linecount);
		}
		return true;
	}

	private static boolean varDecl() throws Exception {
		if (!typeDecl())
			return false;

		ident();
		while (sc.currentToken == ScannerUtils.commaToken) {
			sc.next();
			ident();
		}
		if (!(sc.currentToken == ScannerUtils.semiToken))
			Utils.error("Expected ; token, but got " + sc.token + " at line " + sc.linecount);
		sc.next();
		return true;
	}

	public static void computation(MyScanner myScan) throws Exception {
		sc = myScan;

		if (sc.currentToken == ScannerUtils.mainToken) {
			sc.next();
			while (varDecl())
				;
			while (funcDecl())
				;
			if (sc.currentToken == ScannerUtils.beginToken) {
				sc.next();
				statSequence();
				if (sc.currentToken == ScannerUtils.endToken) {
					sc.next();
					if (sc.currentToken != ScannerUtils.periodToken)
						Utils.error("Expected period token, got " + sc.token + " at line " + sc.linecount);
				} else
					Utils.error("Expected } got " + sc.token + " at line " + sc.linecount);
			}
		} else
			Utils.error("Expected main token, got " + sc.token + " at line " + sc.linecount);
	}
}