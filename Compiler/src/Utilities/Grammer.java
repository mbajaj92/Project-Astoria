package Utilities;

public class Grammer {
	private static MyScanner sc;

	private static void relOp() throws Exception {
		switch (sc.currentToken) {
		case ScannerUtils.leqToken:
		case ScannerUtils.neqToken:
		case ScannerUtils.eqlToken:
		case ScannerUtils.geqToken:
		case ScannerUtils.gtrToken:
		case ScannerUtils.lssToken:
			sc.next();
			break;
		default:
			Utils.error("Expected Relaional Operator Token, got " + sc.currentToken);
		}
	}

	private static void relation() throws Exception {
		expression();
		relOp();
		expression();
	}

	private static void expression() throws Exception {
		term();
		while (sc.currentToken == ScannerUtils.plusToken || sc.currentToken == ScannerUtils.minusToken) {
			sc.next();
			term();
		}
	}

	private static void term() throws Exception {
		factor();
		while (sc.currentToken == ScannerUtils.timesToken || sc.currentToken == ScannerUtils.divToken) {
			sc.next();
			factor();
		}
	}

	private static void factor() throws Exception {
		switch (sc.currentToken) {
		case ScannerUtils.ident:
			designator();
			break;
		case ScannerUtils.number:
			number();
			break;
		case ScannerUtils.openparanToken:
			sc.next();
			expression();
			if (sc.currentToken != ScannerUtils.closeparanToken)
				Utils.error("Expected ) token, got " + sc.currentToken);
			sc.next();
			break;
		case ScannerUtils.callToken:
			funcCall();
			break;
		default:
			Utils.error("Wrong token " + sc.currentToken + " sent to Function Call");
		}
	}

	private static void designator() throws Exception {
		ident();
		while (sc.currentToken == ScannerUtils.openbracketToken) {
			sc.next();
			expression();
			if (sc.currentToken != ScannerUtils.closebracketToken)
				Utils.error("Expected token ], got " + sc.currentToken);
			sc.next();
		}
	}

	private static void returnStatement() throws Exception {
		if (sc.currentToken != ScannerUtils.returnToken)
			Utils.error("Expected return token, got " + sc.currentToken);
		sc.next();

		expression();
	}

	private static void whileStatement() throws Exception {
		if (sc.currentToken != ScannerUtils.whileToken)
			Utils.error("Expected while token, got " + sc.currentToken);
		sc.next();

		relation();
		if (sc.currentToken == ScannerUtils.doToken) {
			statSequence();
			if (sc.currentToken != ScannerUtils.odToken)
				Utils.error("Expected od token, got " + sc.currentToken);
			sc.next();
		} else
			Utils.error("Expected do token, got " + sc.currentToken);
	}

	private static void ifStatement() throws Exception {
		if (sc.currentToken != ScannerUtils.ifToken)
			Utils.error("Expected If token, got " + sc.currentToken);
		sc.next();

		relation();
		if (sc.currentToken == ScannerUtils.thenToken) {
			statSequence();
			if (sc.currentToken == ScannerUtils.elseToken) {
				statSequence();
				while (sc.currentToken == ScannerUtils.elseToken)
					statSequence();
				if (sc.currentToken == ScannerUtils.fiToken) {
					sc.next();
				} else
					Utils.error("Expected fi token, got " + sc.currentToken);
			} else
				Utils.error("Expected else token, got " + sc.currentToken);
		} else
			Utils.error("Expected then token, got " + sc.currentToken);
	}

	private static void funcCall() throws Exception {
		if (sc.currentToken != ScannerUtils.callToken)
			Utils.error("Expected call token, got " + sc.currentToken);
		sc.next();

		ident();
		if (sc.currentToken == ScannerUtils.openparanToken) {
			expression();
			while (sc.currentToken == ScannerUtils.commaToken) {
				expression();
			}
			if (sc.currentToken == ScannerUtils.closeparanToken) {
				sc.next();
			} else
				Utils.error("Expected ), got " + sc.currentToken);

			while (sc.currentToken == ScannerUtils.openparanToken) {
				expression();
				while (sc.currentToken == ScannerUtils.commaToken) {
					expression();
				}
				if (sc.currentToken == ScannerUtils.closeparanToken) {
					sc.next();
				} else
					Utils.error("Expected ), got " + sc.currentToken);
			}
		} else
			Utils.error("Expected (, got " + sc.currentToken);
	}

	private static void assignment() throws Exception {
		if (sc.currentToken != ScannerUtils.letToken)
			Utils.error("Expected let token, got " + sc.currentToken);
		sc.next();

		designator();
		if (sc.currentToken == ScannerUtils.becomesToken) {
			expression();
		} else
			Utils.error("Expected <-, got " + sc.currentToken);
	}

	private static boolean statement() throws Exception {
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
		default:
			return false;
		}
		return true;
	}

	private static boolean statSequence() throws Exception {
		if (!statement())
			return false;

		while (sc.currentToken == ScannerUtils.semiToken) {
			sc.next();
			statement();
		}
		return true;
	}

	private static void funcBody() throws Exception {
		while (varDecl())
			;
		if (sc.currentToken == ScannerUtils.beginToken) {
			if (!statSequence())
				Utils.error("There needs to be atleast 1 valid statement !!");

			while (statSequence())
				;
		}
	}

	private static boolean formalParam() throws Exception {
		if (sc.currentToken != ScannerUtils.openparanToken)
			return false;
		sc.next();
		ident();
		while (sc.currentToken == ScannerUtils.commaToken)
			ident();
		if (sc.currentToken != ScannerUtils.closeparanToken)
			Utils.error("Expected ), but found " + sc.currentToken);
		sc.next();
		return true;
	}

	private static boolean funcDecl() throws Exception {
		if (sc.currentToken == ScannerUtils.funcToken || sc.currentToken == ScannerUtils.procToken) {
			sc.next();
			ident();
			formalParam();
			while (formalParam())
				;
			if (sc.currentToken == ScannerUtils.semiToken) {
				sc.next();
				funcBody();
				if (!(sc.currentToken == ScannerUtils.semiToken))
					Utils.error("Expected ; but found " + sc.currentToken);
				sc.next();
			} else
				Utils.error("Expected ; but found " + sc.currentToken);
			return true;
		}
		return false;
	}

	private static void ident() throws Exception {
		if (sc.currentToken != ScannerUtils.ident)
			Utils.error("Expected Identifier, got " + sc.currentToken);
		sc.next();
	}

	private static void number() throws Exception {
		if (sc.currentToken != ScannerUtils.number)
			Utils.error("Expected number, got " + sc.currentToken);
		sc.next();
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
					Utils.error("Expected ], but got " + sc.currentToken);
				while (sc.currentToken == ScannerUtils.openbracketToken) {
					number();
					if (sc.currentToken == ScannerUtils.closebracketToken) {
						sc.next();
					} else
						Utils.error("Expected ], but got " + sc.currentToken);
				}
			} else
				Utils.error("Expected [, but got " + sc.currentToken);
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
			Utils.error("Expected ; but token found = " + sc.currentToken);
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
					if(sc.currentToken != ScannerUtils.periodToken)
						Utils.error("Expected period token, got "+sc.currentToken);
				} else
					Utils.error("Expected } got " + sc.currentToken);
			}
		} else
			Utils.error("Expected main token, got " + sc.currentToken);
	}
}