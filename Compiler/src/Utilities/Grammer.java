package Utilities;

import java.util.ArrayList;

import Utilities.MyScanner.VARIABLE_TYPE;
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
			Utils.error("Expected Relational Operator Token, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
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
				Utils.error("Expected ) token, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
			sc.next();
			break;
		case ScannerUtils.callToken:
			X = funcCall();
			break;
		}
		return X;
	}

	private static Result designator() throws Exception {
		Result X = ident();
		while (sc.currentToken == ScannerUtils.openbracketToken) {
			sc.next();
			if (!X.isArray) {
				X.expresssions = new ArrayList<Result>();
				X.isArray = true;
			}
			X.expresssions.add(expression());
			if (sc.currentToken != ScannerUtils.closebracketToken)
				Utils.error("Expected token ], got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
			sc.next();
		}
		return X;
	}

	private static void returnStatement() throws Exception {
		if (sc.currentToken != ScannerUtils.returnToken)
			Utils.error("Expected return token, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
		sc.next();

		expression();
	}

	private static void whileStatement() throws Exception {
		if (sc.currentToken != ScannerUtils.whileToken)
			Utils.error("Expected while token, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
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
			Instruction.getInstruction(CODE.BRA);
			doBlock.setChild(loopHeader, false);
			loopHeader.generetePhiAndUpdateTree(previousBlock.getLastAccessTable(), doBlock.getLastAccessTable(),
					previousBlock.getAnchor());
			BasicBlock followBlock = new BasicBlock("LOOP_FOLLOW_"+Utils.WHILE_DEPTH);
			loopHeader.setChild(followBlock, true);
			if (sc.currentToken != ScannerUtils.odToken)
				Utils.error("Expected od token, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
			Utils.WHILE_DEPTH--;
			sc.next();
		} else
			Utils.error("Expected do token, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
	}

	private static void ifStatement() throws Exception {
		if (sc.currentToken != ScannerUtils.ifToken)
			Utils.error("Expected If token, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
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
				Instruction.getInstruction(CODE.BRA);
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
				Utils.error("Expected fi token, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
		} else
			Utils.error("Expected then token, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
	}

	private static Result funcCall() throws Exception {
		if (sc.currentToken != ScannerUtils.callToken)
			Utils.error("Expected call token, got " + sc.token + " at line "
					+ ScannerUtils.getCurrentScanner().getLineCount());
		Result X = new Result();
		sc.setCurrentFunction(Utils.MAIN_FUNC);
		sc.setVarType(VARIABLE_TYPE.FUNC);
		sc.next();
		X = ident();
		X.isFunc = true;
		sc.setVarType(VARIABLE_TYPE.NONE);
		sc.setCurrentFunction(BasicBlock.getCurrentBasicBlock().getFunctionName());
		if (sc.currentToken == ScannerUtils.openparanToken) {
			X.expresssions = new ArrayList<Result>();
			sc.next();
			X.expresssions.add(expression());
			while (sc.currentToken == ScannerUtils.commaToken) {
				sc.next();
				X.expresssions.add(expression());
			}

			if (sc.currentToken == ScannerUtils.closeparanToken) {
				sc.next();
			} else
				Utils.error(
						"Expected ), got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
		}
		X.instruction = Instruction.getInstruction(CODE.call, "&" + X.addressIfVariable, null, true)
				.setFunctionParameters(X.expresssions);
		X.kind = RESULT_KIND.INSTRUCTION;
		return X;
	}

	private static void assignment() throws Exception {
		if (sc.currentToken != ScannerUtils.letToken)
			Utils.error("Expected let token, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
		sc.next();
		Result X = designator();
		if (sc.currentToken == ScannerUtils.becomesToken) {
			sc.next();
			Result Y = expression();
			Utils.becomes(X, Y);
		} else
			Utils.error("Expected <-, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
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
				Utils.error("Expected } token, but got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
			sc.next();
		} else
			Utils.error("Expected { token, but got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
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
				Utils.error("Expected ), but found " + sc.token + " at line "
						+ ScannerUtils.getCurrentScanner().getLineCount());
			sc.next();
		}
		Utils.updateParamSize(X.addressIfVariable, size);
	}

	private static boolean funcDecl() throws Exception {
		if (sc.currentToken == ScannerUtils.funcToken || sc.currentToken == ScannerUtils.procToken) {
			ScannerUtils.getCurrentScanner().setCurrentFunction(Utils.MAIN_FUNC);
			sc.setVarType(VARIABLE_TYPE.FUNC);
			sc.next();
			Result X = ident();
			ScannerUtils.getCurrentScanner().setCurrentFunction(Instruction.toStringConstant("&" + X.addressIfVariable, Utils.MAIN_FUNC));
			new BasicBlock("PROC_START", Instruction.toStringConstant("&" + X.addressIfVariable, Utils.MAIN_FUNC));
			sc.setVarType(VARIABLE_TYPE.FUNC_PARAMS);
			formalParam(X);
			sc.setVarType(VARIABLE_TYPE.NONE);

			if (sc.currentToken == ScannerUtils.semiToken) {
				sc.next();

				funcBody();
				if (!(sc.currentToken == ScannerUtils.semiToken))
					Utils.error("Expected ; but found " + sc.token);
				sc.next();
			} else
				Utils.error("Expected ; but found " + sc.token);
			BasicBlock.getCurrentBasicBlock().setLast();
			return true;
		}
		return false;
	}

	private static Result ident() throws Exception {
		if (sc.currentToken != ScannerUtils.ident)
			Utils.error("Expected Identifier, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
		Result X = new Result();
		X.kind = RESULT_KIND.VAR;
		X.addressIfVariable = sc.id;
		sc.next();
		return X;
	}

	private static Result number() throws Exception {
		if (sc.currentToken != ScannerUtils.number)
			Utils.error("Expected number, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
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
			sc.setVarType(VARIABLE_TYPE.VAR);
			valuesForArray = null;
			sc.next();
		} else if (sc.currentToken == ScannerUtils.arrToken) {
			sc.setVarType(VARIABLE_TYPE.ARR);
			sc.next();
			if (sc.currentToken == ScannerUtils.openbracketToken) {
				sc.next();
				valuesForArray = new ArrayList<Integer>();

				valuesForArray.add(number().valueIfConstant);
				if (sc.currentToken == ScannerUtils.closebracketToken) {
					sc.setValues(valuesForArray);
					sc.next();
				} else
					Utils.error("Expected ], but got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());

				while (sc.currentToken == ScannerUtils.openbracketToken) {
					sc.next();
					valuesForArray.add(number().valueIfConstant);
					if (sc.currentToken == ScannerUtils.closebracketToken) {
						sc.setValues(valuesForArray);
						sc.next();
					} else
						Utils.error("Expected ], but got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
				}
			} else
				Utils.error("Expected [, but got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
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
			Utils.error("Expected ; token, but got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
		sc.setVarType(VARIABLE_TYPE.NONE);
		sc.next();
		return true;
	}

	public static void computation(MyScanner myScan) throws Exception {
		sc = myScan;

		if (sc.currentToken == ScannerUtils.mainToken) {
			BasicBlock buildingBlock = new BasicBlock("INIT_BLOCK", Utils.MAIN_FUNC);
			sc.next();
			ScannerUtils.getCurrentScanner().setCurrentFunction(Utils.MAIN_FUNC);
			while (varDecl())
				;
			while (funcDecl())
				;
			ScannerUtils.getCurrentScanner().setCurrentFunction(Utils.MAIN_FUNC);
			BasicBlock secondBlock = new BasicBlock("INIT_CONT");
			buildingBlock.setChild(secondBlock, true);

			if (sc.currentToken == ScannerUtils.beginToken) {
				sc.next();
				statSequence();
				if (sc.currentToken == ScannerUtils.endToken) {
					sc.next();
					if (sc.currentToken != ScannerUtils.periodToken)
						Utils.error("Expected period token, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
				} else
					Utils.error("Expected } got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
			}
		} else
			Utils.error("Expected main token, got " + sc.token + " at line " + ScannerUtils.getCurrentScanner().getLineCount());
		BasicBlock.getCurrentBasicBlock().setLast();
	}

	public static void handleFollowBlockForIf(BasicBlock prev, BasicBlock ifHeader, BasicBlock thenBlock,
			BasicBlock elseBlock) throws Exception {
		boolean isFollow = true;
		BasicBlock leftBlock = null, rightBlock, followBlock = new BasicBlock("FOLLOW_BLOCK");

		if (elseBlock == null) {
			rightBlock = ifHeader;
		} else if (elseBlock.hasInstructions()) {
			rightBlock = elseBlock;
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
		}

		leftBlock.setChild(followBlock, false);
		rightBlock.setChild(followBlock, false);
		if (isFollow)
			followBlock.updateAnchorLastAccessAndPhi(ifHeader, leftBlock.getLastAccessTable(),
					rightBlock.getLastAccessTable());
	}
}