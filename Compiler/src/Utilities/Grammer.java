package Utilities;

import java.util.ArrayList;

import Utilities.MyScanner.CLASS;
import Utilities.Utils.CODE;
import Utilities.Utils.RESULT_KIND;

public class Grammer {
	private static MyScanner sc;
	private static ArrayList<Integer> valuesForArray = null;

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
			Utils.error("Expected Relational Operator Token, got " + sc.token + " at line " + MyScanner.getLineCount());
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
				Utils.error("Expected ) token, got " + sc.token + " at line " + MyScanner.getLineCount());
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
			if (!X.isArray) {
				X.arrayExp = new ArrayList<Result>();
				X.isArray = true;
			}
			X.arrayExp.add(expression());
			if (sc.currentToken != ScannerUtils.closebracketToken)
				Utils.error("Expected token ], got " + sc.token + " at line " + MyScanner.getLineCount());
			sc.next();
		}
		return X;
	}

	private static void returnStatement() throws Exception {
		if (sc.currentToken != ScannerUtils.returnToken)
			Utils.error("Expected return token, got " + sc.token + " at line " + MyScanner.getLineCount());
		sc.next();

		expression();
	}

	private static void whileStatement() throws Exception {
		if (sc.currentToken != ScannerUtils.whileToken)
			Utils.error("Expected while token, got " + sc.token + " at line " + MyScanner.getLineCount());
		Utils.WHILE_DEPTH++;
		sc.next();

		BasicBlock previousBlock = BasicBlock.getCurrentBasicBlock();
		BasicBlock loopHeader = new BasicBlock("LOOP_HEADER_"+Utils.WHILE_DEPTH);
		previousBlock.setChild(loopHeader, false);
		relation();

		if (sc.currentToken == ScannerUtils.doToken) {
			sc.next();
			BasicBlock doBlock = new BasicBlock("DO_BLOCK");
			loopHeader.setChild(doBlock, true);
			statSequence();
			doBlock = BasicBlock.getCurrentBasicBlock();
			Instruction.getInstruction(CODE.BSR);
			doBlock.setChild(loopHeader, false);
			loopHeader.generetePhiAndUpdateTree(previousBlock.getLastAccessTable(), doBlock.getLastAccessTable(),
					previousBlock.getAnchor());
			BasicBlock followBlock = new BasicBlock("LOOP_FOLLOW_"+Utils.WHILE_DEPTH);
			loopHeader.setChild(followBlock, true);
			if (sc.currentToken != ScannerUtils.odToken)
				Utils.error("Expected od token, got " + sc.token + " at line " + MyScanner.getLineCount());
			Utils.WHILE_DEPTH--;
			sc.next();
		} else
			Utils.error("Expected do token, got " + sc.token + " at line " + MyScanner.getLineCount());
	}

	private static void ifStatement() throws Exception {
		if (sc.currentToken != ScannerUtils.ifToken)
			Utils.error("Expected If token, got " + sc.token + " at line " + MyScanner.getLineCount());
		sc.next();

		BasicBlock prevBlock = BasicBlock.getCurrentBasicBlock();
		BasicBlock ifHeader = new BasicBlock("IF_HEADER");
		prevBlock.setChild(ifHeader, true);
		relation();

		BasicBlock thenBlock = new BasicBlock("THEN_BLOCK");
		ifHeader.setChild(thenBlock, true);

		if (sc.currentToken == ScannerUtils.thenToken) {
			sc.next();
			statSequence();
			thenBlock = BasicBlock.getCurrentBasicBlock();
			BasicBlock elseBlock = null;

			if (sc.currentToken == ScannerUtils.elseToken) {
				Instruction.getInstruction(CODE.BSR);
				sc.next();
				elseBlock = new BasicBlock("ELSE_BLOCK");
				ifHeader.setChild(elseBlock, true);
				statSequence();
				elseBlock = BasicBlock.getCurrentBasicBlock();
			}

			handleFollowBlockForIf(prevBlock, ifHeader, thenBlock, elseBlock);

			if (sc.currentToken == ScannerUtils.fiToken)
				sc.next();
			else
				Utils.error("Expected fi token, got " + sc.token + " at line " + MyScanner.getLineCount());
		} else
			Utils.error("Expected then token, got " + sc.token + " at line " + MyScanner.getLineCount());
	}

	private static void funcCall() throws Exception {
		if (sc.currentToken != ScannerUtils.callToken)
			Utils.error("Expected call token, got " + sc.token + " at line " + MyScanner.getLineCount());
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
				Utils.error("Expected ), got " + sc.token + " at line " + MyScanner.getLineCount());
		}
	}

	private static void assignment() throws Exception {
		if (sc.currentToken != ScannerUtils.letToken)
			Utils.error("Expected let token, got " + sc.token + " at line " + MyScanner.getLineCount());
		sc.next();
		Result X = designator();
		if (sc.currentToken == ScannerUtils.becomesToken) {
			sc.next();
			Result Y = expression();
			Utils.becomes(X, Y);
		} else
			Utils.error("Expected <-, got " + sc.token + " at line " + MyScanner.getLineCount());
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
				Utils.error("Expected } token, but got " + sc.token + " at line " + MyScanner.getLineCount());
			sc.next();
		} else
			Utils.error("Expected { token, but got " + sc.token + " at line " + MyScanner.getLineCount());
	}

	private static void formalParam(Result X) throws Exception {
		int size = 0;
		if (sc.currentToken == ScannerUtils.openparanToken) {
			sc.next();

			if (sc.currentToken == ScannerUtils.ident) {
				sc.next();
				size++;
				while (sc.currentToken == ScannerUtils.commaToken) {
					sc.next();
					ident();
					size++;
				}
			}

			if (sc.currentToken != ScannerUtils.closeparanToken)
				Utils.error("Expected ), but found " + sc.token + " at line " + MyScanner.getLineCount());
			sc.next();
		}
		Utils.updateParamSize(X.addressIfVariable, size);
	}

	private static boolean funcDecl() throws Exception {
		if (sc.currentToken == ScannerUtils.funcToken || sc.currentToken == ScannerUtils.procToken) {
			sc.next();
			sc.mCurrentClass = CLASS.FUNC;
			Result X = ident();
			sc.mCurrentClass = CLASS.NONE;
			formalParam(X);

			if (sc.currentToken == ScannerUtils.semiToken) {
				sc.next();

				new BasicBlock("PROC_START", Instruction.toStringConstant("&" + X.addressIfVariable));
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
			Utils.error("Expected Identifier, got " + sc.token + " at line " + MyScanner.getLineCount());
		Result X = new Result();
		X.kind = RESULT_KIND.VAR;
		X.addressIfVariable = sc.id;
		sc.next();
		return X;
	}

	private static Result number() throws Exception {
		if (sc.currentToken != ScannerUtils.number)
			Utils.error("Expected number, got " + sc.token + " at line " + MyScanner.getLineCount());
		Result X = new Result();
		X.kind = RESULT_KIND.CONST;
		X.valueIfConstant = sc.val;
		sc.next();
		return X;
	}

	private static boolean typeDecl() throws Exception {
		if (sc.currentToken != ScannerUtils.varToken && sc.currentToken != ScannerUtils.arrToken)
			return false;
		if (sc.currentToken == ScannerUtils.varToken) {
			sc.mCurrentClass = CLASS.VAR;
			valuesForArray = null;
			sc.next();
		} else if (sc.currentToken == ScannerUtils.arrToken) {
			sc.mCurrentClass = CLASS.ARR;
			sc.next();
			if (sc.currentToken == ScannerUtils.openbracketToken) {
				sc.next();
				valuesForArray = new ArrayList<Integer>();

				valuesForArray.add(number().valueIfConstant);
				if (sc.currentToken == ScannerUtils.closebracketToken) {
					sc.setValues(valuesForArray);
					sc.next();
				} else
					Utils.error("Expected ], but got " + sc.token + " at line " + MyScanner.getLineCount());

				while (sc.currentToken == ScannerUtils.openbracketToken) {
					sc.next();
					valuesForArray.add(number().valueIfConstant);
					if (sc.currentToken == ScannerUtils.closebracketToken) {
						sc.setValues(valuesForArray);
						sc.next();
					} else
						Utils.error("Expected ], but got " + sc.token + " at line " + MyScanner.getLineCount());
				}
			} else
				Utils.error("Expected [, but got " + sc.token + " at line " + MyScanner.getLineCount());
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
		sc.setValues(null);
		if (!(sc.currentToken == ScannerUtils.semiToken))
			Utils.error("Expected ; token, but got " + sc.token + " at line " + MyScanner.getLineCount());
		sc.next();
		sc.mCurrentClass = CLASS.NONE;
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
			new BasicBlock("INIT_BLOCK", "MAIN");
			if (sc.currentToken == ScannerUtils.beginToken) {
				sc.next();
				statSequence();
				if (sc.currentToken == ScannerUtils.endToken) {
					sc.next();
					if (sc.currentToken != ScannerUtils.periodToken)
						Utils.error("Expected period token, got " + sc.token + " at line " + MyScanner.getLineCount());
				} else
					Utils.error("Expected } got " + sc.token + " at line " + MyScanner.getLineCount());
			}
		} else
			Utils.error("Expected main token, got " + sc.token + " at line " + MyScanner.getLineCount());
	}

	public static void handleFollowBlockForIf(BasicBlock prev, BasicBlock ifHeader, BasicBlock thenBlock,
			BasicBlock elseBlock) throws Exception {
		boolean isFollow = true;
		BasicBlock leftBlock = null, rightBlock, followBlock = new BasicBlock("FOLLOW_BLOCK");

		if (elseBlock == null) {
			rightBlock = ifHeader;
			ifHeader.setChild(followBlock, false);
		} else if (elseBlock.hasInstructions()) {
			rightBlock = elseBlock;
			elseBlock.setChild(followBlock, false);
		} else {
			rightBlock = ifHeader;
		}

		if (!thenBlock.hasInstructions()) {
			if (rightBlock != ifHeader) {
				leftBlock = ifHeader;
				isFollow = true;
			} else {
				thenBlock.ignore();
				isFollow = false;
			}
		} else {
			leftBlock = thenBlock;
			thenBlock.setChild(followBlock, false);
		}

		if (isFollow)
			followBlock.updateAnchorLastAccessAndPhi(ifHeader, leftBlock.getLastAccessTable(),
					rightBlock.getLastAccessTable());
	}
}