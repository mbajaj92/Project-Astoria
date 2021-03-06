package Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import Utilities.MyScanner.VARIABLE_TYPE;

public class Utils {
	public static HashMap<Integer, BasicBlock> leftOverTrace = null;
	public static final String OUTPUT_NUM = "OutputNum";
	public static final String OUTPUT_NL = "OutputNewLine";
	public static final String INPUT_NUM = "InputNum";
	public static final String MAIN_FUNC = "MAIN$FUNC";
	public static int WHILE_DEPTH = 0;
	public static final boolean COM_SUBEX_ELIM = true;
	public static final boolean COPY_PROP = true;
	public static final int MACHINE_BYTE_SIZE = 4;
	public static List<CODE> isNotDeadCode = Arrays.asList(CODE.write, CODE.writeNL, CODE.read, CODE.CMP, CODE.BLE,
			CODE.BGE, CODE.BEQ, CODE.BGT, CODE.BLT, CODE.BNE, CODE.BSR, CODE.CMPI, CODE.store, CODE.RET, CODE.call, CODE.EOF);
	public static List<CODE> doNotTestAnchor = Arrays.asList(CODE.CMP, CODE.CMPI, CODE.BSR, CODE.BEQ, CODE.BNE,
			CODE.BLT, CODE.BGE, CODE.BLE, CODE.BGT, CODE.store, CODE.call, CODE.read, CODE.write, CODE.writeNL, CODE.RET);
	public static List<CODE> compareInstructions = Arrays.asList(CODE.CMP, CODE.CMPI, CODE.BSR, CODE.BEQ, CODE.BNE,
			CODE.BLT, CODE.BGE, CODE.BLE, CODE.BGT);
	public static List<CODE> doNotCreateEdge = Arrays.asList(CODE.BSR, CODE.BEQ, CODE.BNE,
			CODE.BLT, CODE.BGE, CODE.BLE, CODE.BGT);
	private static List<CODE> F1Instructions = Arrays.asList(CODE.ADDI, CODE.SUBI, CODE.MULI, CODE.DIVI, CODE.MODI,
			CODE.CMPI, CODE.ORI, CODE.ANDI, CODE.BICI, CODE.XORI, CODE.LSHI, CODE.ASHI, CODE.CHKI);
	private static List<CODE> F2Instructions = Arrays.asList(CODE.ADD, CODE.SUB, CODE.MUL, CODE.DIV, CODE.MOD, CODE.CMP,
			CODE.OR, CODE.AND, CODE.BIC, CODE.XOR, CODE.LSH, CODE.ASH, CODE.CHK);
	private static HashMap<String, int[]> metaStackAndFramePointerData = null;
	private static HashMap<String, HashMap<String, Integer>> metaIDTable = null;
	private static HashMap<String, HashMap<Integer, ArrayList<Integer>>> metaArrayTable = null;
	private static HashMap<Integer, HashSet<Integer>> interfearanceGraph = null;
	private static HashMap<Integer, Integer> funcInfoTable = null;
	private static HashMap<String, Integer> functionList = null;
	private static HashMap<Integer, HashSet<Integer>> phiClusters = null;
	private static LinkedHashMap<Integer, ArrayList<Boolean>> color_mapping = null;
	private static HashMap<Integer, HashSet<Integer>> fixUpMap = null;

	public static void nullCheck() {
		if (metaIDTable == null)
			metaIDTable = new HashMap<String, HashMap<String, Integer>>();

		if (metaArrayTable == null)
			metaArrayTable = new HashMap<String, HashMap<Integer, ArrayList<Integer>>>();

		if (funcInfoTable == null)
			funcInfoTable = new HashMap<Integer, Integer>();

		if (interfearanceGraph == null)
			interfearanceGraph = new HashMap<Integer, HashSet<Integer>>();

		if(functionList == null)
			functionList = new HashMap<String,Integer>();

		if (metaStackAndFramePointerData == null) {
			metaStackAndFramePointerData = new HashMap<String, int[]>();
			int[] init = new int[2];
			/* Frame Pointer */
			init[0] = 0;
			/* Stack Pointer */
			init[1] = 0;
			metaStackAndFramePointerData.put(Utils.MAIN_FUNC, init);
		}
		
		if (leftOverTrace == null)
			leftOverTrace = new HashMap<Integer, BasicBlock>();

		if (phiClusters == null)
			phiClusters = new HashMap<Integer, HashSet<Integer>>();
		
		if (fixUpMap == null)
			fixUpMap = new HashMap<Integer, HashSet<Integer>>();
	}

	public static String getFunctionForIdentifier(String id/* , String func */) throws Exception {
		return id.split("_")[1];
	}

	public static String address2Identifier(String id) throws Exception {
		return address2IdentifierImpl(id)[0];
	}
	
	public static String[] address2IdentifierImpl(String in) throws Exception {
		nullCheck();
		String returnValue[] = new String[2];
		int id = Integer.parseInt(in.split("_")[0]);
		String functionName = in.split("_")[1];

		/* Utils.SOPln("Asking to convert "+id+" in "+functionName); */
		HashMap<String, Integer> idTable = metaIDTable.get(functionName);
		if (idTable != null) {
			for (String key : idTable.keySet()) {
				if (id == idTable.get(key)) {
					returnValue[0] = key;
					returnValue[1] = functionName;
					return returnValue;
				}
			}
		} else
			SOPln("ID TABLE FOR " + functionName + " not found");

		if (functionName.equals(MAIN_FUNC)) {
			for (String key : functionList.keySet()) {
				if (id == functionList.get(key)) {
					returnValue[0] = key;
					returnValue[1] = MAIN_FUNC;
					return returnValue;
				}
			}
		}
		Utils.error("ID " + id + " NOT FOUND at " + ScannerUtils.getCurrentScanner().getLineCount() + " funName is "
				+ functionName);
		return null;
	}

	public static void updateParamSize(String id, int paramSize) {
		funcInfoTable.put(Integer.parseInt(id.split("_")[0]), paramSize);
	}

	public static int getFramePointerFor(String func) throws Exception {
		if (!metaStackAndFramePointerData.containsKey(func))
			error("Function " + func + " not defined");
		return metaStackAndFramePointerData.get(func)[0];
	}

	public static int getStackPointerFor(String func) throws Exception {

		if (!metaStackAndFramePointerData.containsKey(func))
			error("Function " + func + " not defined at line " + ScannerUtils.getCurrentScanner().getLineCount());
		return metaStackAndFramePointerData.get(func)[1];
	}

	public static void updateFramePointerFor(String func, int fp) {
		int values[];
		if (!metaStackAndFramePointerData.containsKey(func)) {
			values = new int[2];
			metaStackAndFramePointerData.put(func, values);
		} else
			values = metaStackAndFramePointerData.get(func);
		values[0] = fp;
	}

	public static void updateStackPointerFor(String func, int sp) {
		int values[];
		if (!metaStackAndFramePointerData.containsKey(func)) {
			values = new int[2];
			metaStackAndFramePointerData.put(func, values);
		} else
			values = metaStackAndFramePointerData.get(func);
		values[1] = sp;
	}

	public static String identifier2AddressNew(String name) throws Exception {
		return identifier2AddressNew(name, null, VARIABLE_TYPE.NONE);
	}

	public static String identifier2AddressNew(String name, ArrayList<Integer> arrayInfo, VARIABLE_TYPE mVarType)
			throws Exception {
		nullCheck();

		String currentFunc = ScannerUtils.getCurrentScanner().getCurrentFunction();
		HashMap<String, Integer> idTable = null;
		if (!metaIDTable.containsKey(currentFunc)) {
			/* SOPln("Creating ID Table for "+currentFunc); */
			idTable = new HashMap<String, Integer>();
			metaIDTable.put(currentFunc, idTable);
		} else
			idTable = metaIDTable.get(currentFunc);

		if (mVarType == VARIABLE_TYPE.FUNC) {
			/* SOPln("Looking for Function "+name); */
			if (functionList.containsKey(name)) {
				String returnVal = "";
				/* function already defined */
				/* SOPln("Function "+name+" already defined "); */
				returnVal = "" + functionList.get(name);
				returnVal += "_" + MAIN_FUNC;
				return returnVal;
			} else {
				/* function needs to be defined */
				/* SOPln("Function "+name+" is being defined "); */
				updateFramePointerFor(name, 0);
				updateStackPointerFor(name, 0);
				int value = getStackPointerFor(currentFunc);
				updateStackPointerFor(currentFunc, value + MACHINE_BYTE_SIZE);
				funcInfoTable.put(value, 0);
				functionList.put(name, value);
				SOPln(name + " - [" + value + "_" + MAIN_FUNC + "] | " + currentFunc);
				return value + "_" + MAIN_FUNC;
			}
		} else if (mVarType == VARIABLE_TYPE.NONE) {
			/* ACCESS OLD */
			int returnValue = 0;
			if (idTable.containsKey(name)) { 
				return idTable.get(name)+ "_" + currentFunc;
			} else {
				
				SOPln("Couldn't find " + name + " in the function " + currentFunc + " hence looking for " + MAIN_FUNC);

				idTable = metaIDTable.get(MAIN_FUNC);
				if (idTable.containsKey(name))
					returnValue = idTable.get(name);
				else
					Utils.error("Undefined Identifier " + name + " at line "
							+ ScannerUtils.getCurrentScanner().getLineCount());
				return returnValue+ "_" + MAIN_FUNC;
			}
		} else {
			int returnValue = 0;
			switch (mVarType) {
			case VAR:
				returnValue = getStackPointerFor(currentFunc);
				updateStackPointerFor(currentFunc, returnValue + MACHINE_BYTE_SIZE);
				break;
			case ARR:
				returnValue = getStackPointerFor(currentFunc);
				updateStackPointerFor(currentFunc, returnValue + MACHINE_BYTE_SIZE);
				if (metaArrayTable.containsKey(currentFunc))
					metaArrayTable.get(currentFunc).put(returnValue, arrayInfo);
				else {
					HashMap<Integer, ArrayList<Integer>> table = new HashMap<Integer, ArrayList<Integer>>();
					table.put(returnValue, arrayInfo);
					metaArrayTable.put(currentFunc, table);
				}
				break;
			case FUNC_PARAMS:
				returnValue = -8 + (-MACHINE_BYTE_SIZE * (idTable.size() + 1));
				break;
			default:
				Utils.error("UNKNOWN CASE " + mVarType);
			}
			SOPln(name + " - [" + returnValue + "_" + currentFunc + "] | " + currentFunc);
			idTable.put(name, returnValue);
			return returnValue + "_" + currentFunc;
		}
	}

	public static void SOPln(Object toPrint) {
		SOP(toPrint.toString() + "\n");
	}

	public static void SOP(Object toPrint) {
		System.out.print(toPrint.toString());
	}
	/////////////////////////////////////////////////////////////////////////////////////////////
	public static enum BasicBlockType{
		REGULAR, IF_HEADER, THEN_BLOCK, ELSE_BLOCK, LOOP_FOLLOW, LOOP_HEADER, DO_BLOCK, FOLLOW_BLOCK;
	};
	////////////////////////////////////////////////////////////////////////////////////////////
	public static enum RESULT_KIND {
		NONE, CONST, VAR, INSTRUCTION, CONDITION
	};

	public static enum CODE {
		NONE, ADD, SUB, MUL, DIV, MOD, CMP, OR, AND, BIC, XOR, LSH, ASH, CHK, ADDI, SUBI, MULI, DIVI, MODI, CMPI, ORI, ANDI, BICI, XORI, LSHI, ASHI, CHKI, LDW, LDX, POP, STW, STX, PSH, BEQ, BNE, BLT, BGE, BLE, BGT, BSR, JSR, RET, RDD, WRD, WRH, WRL, adda, move, store, load, phi, call, read, write, writeNL, EOF
	};

	public static int programCounter = 0;
	public static int stackPointer = 0;
	public static ArrayList<Integer> buffer = new ArrayList<Integer>();

	/* TODO: need to fixup; later */
	public static void fixup(int index) {
		SOPln("We are fixing up for index " + index);
		/*
		 * Instruction i = Instruction.instructionList.get(index);
		 * i.fixup(Instruction.getCurrentInstructionIndex() + 1 - index);
		 * SOPln(i);
		 */
	}

	public static void fixupOld(int index, int pc) {
		buffer.add(index, (buffer.remove(index) & 0xffff0000) + ((pc - index) & 0x0000ffff));
	}

	public static void fixupOld(int index) {
		fixupOld(index, programCounter);
	}

	public static CODE negateCondition(CODE cond) throws Exception {
		switch (cond) {
		case BEQ:
			return CODE.BNE;
		case BNE:
			return CODE.BEQ;
		case BLT:
			return CODE.BGE;
		case BGE:
			return CODE.BLT;
		case BLE:
			return CODE.BGT;
		case BGT:
			return CODE.BLE;
		default:
			error("Invalid Conditional Operator !");
		}
		return null;
	}

	public static String format(String input, int length) {
		for (int i = input.length() + 1; i <= length; i++)
			input = "0" + input;
		return input;
	}

	private static void putF3(int code, int c/*, String operation*/) throws Exception {
		String op = Integer.toBinaryString(code);
		op = format(op, 6);
		String cbits = Integer.toBinaryString(c);
		if (cbits.length() > 26)
			error("Length of Cbits is greater than 26 !! ");

		cbits = format(cbits, 26);
		buffer.add(programCounter++, Integer.parseInt(op + cbits,2));
	}

	private static void putF2(int code, int a, int b, int c/*, String operation*/) throws Exception {
		if (a >= 32)
			error("Length of abits is greater than 5 !! ");

		if (b >= 32)
			error("Length of bbits is greater than 5 !! ");

		if (c >= 32)
			error("Length of cbits is greater than 5 !! ");
		implementF1F2(code, a, b, c/*, operation*/);
	}

	private static void implementF1F2(int code, int a, int b,
			int c/* , String operation */) {
		int value = code<<26 | a<<21 | b<<16 | c & 0xffff;
		buffer.add(programCounter++, value);
		/*String op = Integer.toBinaryString(code);
		op = format(op, 6);
		SOPln("op = "+op);
		String abits = Integer.toBinaryString(a);
		abits = format(abits, 5);
		SOPln("abits = "+abits);
		String bbits = Integer.toBinaryString(b);
		bbits = format(bbits, 5);
		SOPln("bbits = "+bbits);
		String cbits = Integer.toBinaryString(c);
		cbits = format(cbits, 16);
		SOPln("cbits = "+cbits);
		Utils.SOPln("LEN = "+(op + abits + bbits + cbits).length());*/
		//
	}

	private static void putF1(int code, int a, int b, int c/*, String operation*/) throws Exception {
		if (a >= 32)
			error("Length of abits is greater than 5 !! ");

		if (b >= 32)
			error("Length of bbits is greater than 5 !! ");

		if (c >= 65536)
			error("Length of cbits is greater than 16 !! ");

		implementF1F2(code, a, b, c/*, operation*/);
	}

	public static void emit(String command) {
		SOPln(command);
	}

	public static void handleF1(Instruction i) throws Exception {
		int a = getRegisterForColor(i.getColor());
		if (i.getLeftInstruction() == null)
			error("LEFT INSTRUCTION UNVAILABLE for "+i.getInstructionNumber());
		int b = getRegisterForColor(i.getLeftInstruction().getColor());
		if (i.getRightConstant() == null)
			error("RIGHT CONSTANT UNVAILABLE for "+i.getInstructionNumber());
		int c = Integer.parseInt((i.getRightConstant().substring(1)));
		put(i.getCode(), a, b, c);
	}

	public static void handleF2(Instruction i) throws Exception {
		int a = getRegisterForColor(i.getColor());
		if (i.getLeftInstruction() == null)
			error("LEFT INSTRUCTION UNVAILABLE");
		int b = getRegisterForColor(i.getLeftInstruction().getColor());
		if (i.getRightInstruction() == null)
			error("RIGHT INSTRUCTION UNVAILABLE");
		int c = getRegisterForColor(i.getRightInstruction().getColor());
		put(i.getCode(), a, b, c);
	}

	public static void put(CODE op, int a, int b, int c) throws Exception {
		if (a < 0 || b < 0 || c < 0)
			error("Invalid Operand, can't have negative operands ");
		switch (op) {
		case CHK:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF2(14, a, b, c/*, op.toString()*/);
			break;
		case ASH:
			putF2(13, a, b, c/*, op.toString()*/);
			break;
		case LSH:
			putF2(12, a, b, c/*, op.toString()*/);
			break;
		case XOR:
			putF2(11, a, b, c/*, op.toString()*/);
			break;
		case BIC:
			putF2(10, a, b, c/*, op.toString()*/);
			break;
		case AND:
			putF2(9, a, b, c/*, op.toString()*/);
			break;
		case OR:
			putF2(8, a, b, c/*, op.toString()*/);
			break;
		case CMP:
			putF2(5, a, b, c/*, op.toString()*/);
			break;
		case MOD:
			putF2(4, a, b, c/*, op.toString()*/);
			break;
		case DIV:
			putF2(3, a, b, c/*, op.toString()*/);
			break;
		case MUL:
			putF2(2, a, b, c/*, op.toString()*/);
			break;
		case SUB:
			putF2(1, a, b, c/*, op.toString()*/);
			break;
		case ADD:
			putF2(0, a, b, c/*, op.toString()*/);
			break;
		case CHKI:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF1(30, a, b, c/*, op.toString()*/);
			break;
		case ASHI:
			putF1(29, a, b, c/*, op.toString()*/);
			break;
		case LSHI:
			putF1(28, a, b, c/*, op.toString()*/);
			break;
		case XORI:
			putF1(27, a, b, c/*, op.toString()*/);
			break;
		case BICI:
			putF1(26, a, b, c/*, op.toString()*/);
			break;
		case ANDI:
			putF1(25, a, b, c/*, op.toString()*/);
			break;
		case ORI:
			putF1(24, a, b, c/*, op.toString()*/);
			break;
		case CMPI:
			putF1(21, a, b, c/*, op.toString()*/);
			break;
		case MODI:
			putF1(20, a, b, c/*, op.toString()*/);
			break;
		case DIVI:
			putF1(19, a, b, c/*, op.toString()*/);
			break;
		case MULI:
			putF1(18, a, b, c/*, op.toString()*/);
			break;
		case SUBI:
			putF1(17, a, b, c/*, op.toString()*/);
			break;
		case ADDI:
			putF1(16, a, b, c/*, op.toString()*/);
			break;
		case LDW:
			putF1(32, a, b, c/*, op.toString()*/);
			break;
		case LDX:
			putF1(33, a, b, c/*, op.toString()*/);
			break;
		case POP:
			putF1(34, a, b, c/*, op.toString()*/);
			break;
		case STW:
			putF1(36, a, b, c/*, op.toString()*/);
			break;
		case STX:
			putF2(37, a, b, c/*, op.toString()*/);
			break;
		case PSH:
			putF1(38, a, b, c/*, op.toString()*/);
			break;
		case BEQ:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF1(40, a, 0, c/*, op.toString()*/);
			break;
		case BNE:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF1(41, a, 0, c/*, op.toString()*/);
			break;
		case BLT:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF1(42, a, 0, c/*, op.toString()*/);
			break;
		case BGE:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF1(43, a, 0, c/*, op.toString()*/);
			break;
		case BLE:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF1(44, a, 0, c/*, op.toString()*/);
			break;
		case BGT:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF1(45, a, 0, c/*, op.toString()*/);
			break;
		case BSR:
			if (a != 0 || b != 0)
				error("Invalid Operand, can't give a and b  ");

			putF1(46, 0, 0, c/*, op.toString()*/);
			break;
		case JSR:
			if (a != 0 || b != 0)
				error("Invalid Operand, can't give a and b  ");

			putF3(48, c/*, op.toString()*/);
			break;
		case RET:
			if (a != 0 || b != 0)
				error("Invalid Operand, can't give a and b  ");

			putF2(49, 0, 0, c/*, op.toString()*/);
			break;
		case RDD:
			if (c != 0 || b != 0)
				error("Invalid Operand, can't give c and b  ");

			putF2(50, a, 0, 0/*, op.toString()*/);
			break;
		case WRD:
			if (c != 0 || a != 0)
				error("Invalid Operand, can't give a and c  ");

			putF2(51, 0, b, 0/*, op.toString()*/);
			break;
		case WRH:
			if (c != 0 || a != 0)
				error("Invalid Operand, can't give a and c  ");

			putF2(51, 0, b, 0/*, op.toString()*/);
			break;
		case WRL:
			if (b != 0 || a != 0 || c != 0)
				error("Invalid Operand, can't give operands");

			putF1(53, 0, 0, 0/*, op.toString()*/);
			break;
		default:
			error("Invalid Operand code ");
		}
	}

	public static void compute(int opCode, Result X, Result Y) throws Exception {
		if (opCode == ScannerUtils.becomesToken)
			error("DO NOT CALL COMPUTE FOR BECOMES");

		if (X.kind == RESULT_KIND.CONST && Y.kind == RESULT_KIND.CONST) {
			switch (opCode) {
			case ScannerUtils.plusToken:
				X.valueIfConstant += Y.valueIfConstant;
				break;
			case ScannerUtils.minusToken:
				X.valueIfConstant -= Y.valueIfConstant;
				break;
			case ScannerUtils.timesToken:
				X.valueIfConstant *= Y.valueIfConstant;
				break;
			case ScannerUtils.divToken:
				X.valueIfConstant /= Y.valueIfConstant;
				break;
			case ScannerUtils.leqToken:
				X.booleanIfCondition = X.valueIfConstant <= Y.valueIfConstant?"TRUE":"FALSE";
				break;
			case ScannerUtils.neqToken:
				X.booleanIfCondition = X.valueIfConstant != Y.valueIfConstant?"TRUE":"FALSE";
				break;
			case ScannerUtils.eqlToken:
				X.booleanIfCondition = X.valueIfConstant == Y.valueIfConstant?"TRUE":"FALSE";
				break;
			case ScannerUtils.geqToken:
				X.booleanIfCondition = X.valueIfConstant >= Y.valueIfConstant?"TRUE":"FALSE";
				break;
			case ScannerUtils.gtrToken:
				X.booleanIfCondition = X.valueIfConstant > Y.valueIfConstant?"TRUE":"FALSE";
				break;
			case ScannerUtils.lssToken:
				X.booleanIfCondition = X.valueIfConstant < Y.valueIfConstant?"TRUE":"FALSE";
				break;
				/*load(X);
				load(Y);
				X.instruction = Instruction.getInstruction(CODE.CMPI, X.instruction, Y.instruction)
						.setAInsFor("&" + X.addressIfVariable).setBInsFor("&" + Y.addressIfVariable);
				X.addressIfVariable = null;
				handleCompare(opCode, X.instruction);
				break;*/
			}
		} else {
			if ((opCode == ScannerUtils.plusToken || opCode == ScannerUtils.timesToken) && X.kind == RESULT_KIND.CONST
					&& Y.kind != RESULT_KIND.CONST) {
				load(Y);
				if ((X.valueIfConstant == 0 && opCode == ScannerUtils.plusToken)
						|| (X.valueIfConstant == 1 && opCode == ScannerUtils.timesToken))
					X.instruction = Y.instruction;
				else
					X.instruction = Instruction.getInstruction(opCode == ScannerUtils.plusToken ? CODE.ADDI : CODE.MULI,
							Y.instruction, "#" + X.valueIfConstant).setAInsFor("&" + Y.addressIfVariable);

				X.kind = RESULT_KIND.INSTRUCTION;

			} else {

				load(X);
				if (Y.kind == RESULT_KIND.CONST) {
					CODE command = null;
					switch (opCode) {
					case ScannerUtils.becomesToken:
						command = CODE.STX;
						break;
					case ScannerUtils.plusToken:
						command = CODE.ADDI;
						break;
					case ScannerUtils.minusToken:
						command = CODE.SUBI;
						break;
					case ScannerUtils.timesToken:
						command = CODE.MULI;
						break;
					case ScannerUtils.divToken:
						command = CODE.DIVI;
						break;
					case ScannerUtils.leqToken:
					case ScannerUtils.neqToken:
					case ScannerUtils.eqlToken:
					case ScannerUtils.geqToken:
					case ScannerUtils.gtrToken:
					case ScannerUtils.lssToken:
						command = CODE.CMPI;
						break;
					}
					X.instruction = Instruction.getInstruction(command, X.instruction, "#" + Y.valueIfConstant)
							.setAInsFor("&" + X.addressIfVariable);
					if (command == CODE.CMPI)
						handleCompare(opCode,X.instruction);

				} else {
					load(Y);
					CODE command = null;
					switch (opCode) {
					case ScannerUtils.plusToken:
						command = CODE.ADD;
						break;
					case ScannerUtils.minusToken:
						command = CODE.SUB;
						break;
					case ScannerUtils.timesToken:
						command = CODE.MUL;
						break;
					case ScannerUtils.divToken:
						command = CODE.DIV;
						break;
					case ScannerUtils.leqToken:
					case ScannerUtils.neqToken:
					case ScannerUtils.eqlToken:
					case ScannerUtils.geqToken:
					case ScannerUtils.gtrToken:
					case ScannerUtils.lssToken:
						command = CODE.CMP;
						break;
					}

					X.instruction = Instruction.getInstruction(command, X.instruction, Y.instruction)
							.setAInsFor("&" + X.addressIfVariable).setBInsFor("&" + Y.addressIfVariable);
					X.addressIfVariable = null;
					if (command == CODE.CMP)
						handleCompare(opCode, X.instruction);
				}
			}
		}
	}

	public static void handleCompare(int code, Instruction i) {
		switch (code) {
		case ScannerUtils.leqToken:
			Instruction.getInstruction(CODE.BGT).setLeftInstruction(i);
			break;
		case ScannerUtils.neqToken:
			Instruction.getInstruction(CODE.BEQ).setLeftInstruction(i);
			break;
		case ScannerUtils.eqlToken:
			Instruction.getInstruction(CODE.BNE).setLeftInstruction(i);
			break;
		case ScannerUtils.geqToken:
			Instruction.getInstruction(CODE.BLT).setLeftInstruction(i);
			break;
		case ScannerUtils.gtrToken:
			Instruction.getInstruction(CODE.BLE).setLeftInstruction(i);
			break;
		case ScannerUtils.lssToken:
			Instruction.getInstruction(CODE.BGE).setLeftInstruction(i);
			break;
		}
	}

	public static void specialLoad(Result X) throws Exception {
		if (X.kind == RESULT_KIND.CONST) {
			X.instruction = Instruction.getInstruction(CODE.ADDI, "#0", "#" + X.valueIfConstant, false);
		} else if (X.kind == RESULT_KIND.VAR && !X.isArray) {
			String FP = "#FP_"+getFunctionForIdentifier(X.addressIfVariable);
			X.instruction = Instruction.getInstruction(CODE.load, FP, /*"&" +*/ X.addressIfVariable, false);
			//Utils.SOPln("Special Load for "+X.addressIfVariable+" "+X.instruction.getRightConstant());
		} else if (X.kind == RESULT_KIND.VAR && X.isArray) {
			Result a = getOffsetForArray(X/*, ScannerUtils.getCurrentScanner().getCurrentFunction()*/);
			Result b = new Result();
			b.kind = RESULT_KIND.CONST;
			b.valueIfConstant = Utils.MACHINE_BYTE_SIZE;
			compute(ScannerUtils.plusToken, a, b);
			load(a);
			String FP = "#FP_"+getFunctionForIdentifier(X.addressIfVariable);
			Instruction two = Instruction.getInstruction(CODE.ADDI, FP, "&" + X.addressIfVariable);
			Instruction three = Instruction.getInstruction(CODE.adda, a.instruction, two);
			X.instruction = Instruction.getInstructionForArray(CODE.load, three, null).setLoadForArray()
					.setAInsFor("&" + X.addressIfVariable);
		}
		X.kind = RESULT_KIND.INSTRUCTION;
	}

	public static void load(Result X) throws Exception {
		if (X.kind == RESULT_KIND.CONST) {
			X.instruction = Instruction.getInstruction(CODE.ADDI, Instruction.getZeroInstruction(), "#" + X.valueIfConstant);
		} else if (X.kind == RESULT_KIND.VAR && !X.isArray) {
			String FP = "#FP_"+getFunctionForIdentifier(X.addressIfVariable);
			X.instruction = Instruction.getInstruction(CODE.load, FP, "&" + X.addressIfVariable);
		} else if (X.kind == RESULT_KIND.VAR && X.isArray) {
			Result a = getOffsetForArray(X/*, ScannerUtils.getCurrentScanner().getCurrentFunction()*/);
			Result b = new Result();
			b.kind = RESULT_KIND.CONST;
			b.valueIfConstant = Utils.MACHINE_BYTE_SIZE;
			compute(ScannerUtils.plusToken, a, b);
			load(a);
			String FP = "#FP_"+getFunctionForIdentifier(X.addressIfVariable);
			Instruction two = Instruction.getInstruction(CODE.ADDI, FP, "&" + X.addressIfVariable);
			Instruction three = Instruction.getInstruction(CODE.adda, a.instruction, two);
			X.instruction = Instruction.getInstructionForArray(CODE.load, three, null).setLoadForArray()
					.setAInsFor("&" + X.addressIfVariable);
		}
		X.kind = RESULT_KIND.INSTRUCTION;
	}

	public static void becomes(Result X, Result Y) throws Exception {

		load(Y);
		if (!X.isArray) {
			X.instruction = Instruction.getInstruction(CODE.move, "&" + X.addressIfVariable, Y.instruction);
			X.instruction.addMoveFor("&" + X.addressIfVariable);

		} else {

			Result a = getOffsetForArray(X/*, ScannerUtils.getCurrentScanner().getCurrentFunction()*/);
			Result b = new Result();
			b.kind = RESULT_KIND.CONST;
			b.valueIfConstant = Utils.MACHINE_BYTE_SIZE;
			compute(ScannerUtils.plusToken, a, b);
			load(a);
			String FP = "#FP_"+getFunctionForIdentifier(X.addressIfVariable);
			Instruction two = Instruction.getInstruction(CODE.ADDI, FP, "&" + X.addressIfVariable);
			Instruction three = Instruction.getInstruction(CODE.adda, a.instruction, two);
			Instruction.getInstructionForArray(CODE.store, Y.instruction, three).setStoreFor("&" + X.addressIfVariable);
		}
	}

	public static boolean isF1(CODE code) {
		return F1Instructions.contains(code);
	}
	
	public static boolean isF2(CODE code) {
		return F2Instructions.contains(code);
	}

	public static boolean isInLoop() {
		return WHILE_DEPTH > 0;
	}
	
	public static boolean isArray(String key) {
		if(key == null)
			return false;

		key = key.substring(1);
		if (!metaArrayTable.containsKey(key.split("_")[1]))
			return false;

		return metaArrayTable.get(key.split("_")[1]).containsKey(key.split("_")[0]);
	}

	public static Result getOffsetForArray(Result X/*, String funcName*/) throws Exception {
		String funcName = X.addressIfVariable.split("_")[1];
		HashMap<Integer, ArrayList<Integer>> arrayInfoTable = metaArrayTable.get(funcName);
		int id = Integer.parseInt(X.addressIfVariable.split("_")[0]);

		if (!arrayInfoTable.containsKey(id))
			Utils.error("ARRAY " + Instruction.toStringConstant("&"+X.addressIfVariable) + " does not exist in "+funcName);

		ArrayList<Integer> indices = arrayInfoTable.get(id);
		if (X.expresssions.size() != indices.size())
			Utils.error("Array index mismatch");

		Result temp = new Result();
		temp.valueIfConstant = 0;
		temp.kind = RESULT_KIND.CONST;

		for (int i = 0; i < X.expresssions.size(); i++) {
			Result r = X.expresssions.get(i);
			if (r.kind == RESULT_KIND.CONST && r.valueIfConstant >= indices.get(i))
				Utils.error("Array Index " + r.valueIfConstant + " out of range " + indices.get(i) + " at line "
						+ ScannerUtils.getCurrentScanner().getLineCount());

			int prod = 1;
			for (int j = i + 1; j < indices.size(); j++)
				prod *= indices.get(j);

			if (prod != 1) {
				Result s = new Result();
				s.valueIfConstant = prod;
				s.kind = RESULT_KIND.CONST;
				compute(ScannerUtils.timesToken, r, s);
			}
			compute(ScannerUtils.plusToken, temp, r);
		}
		return temp;
	}

	public static void error(String errorMsg) throws Exception {
		throw new Exception(errorMsg);
	}

	public static void printArrayTable() {
		for (String key : metaArrayTable.keySet()) {
			Utils.SOPln("Inside function " + key);
			for (Integer innerKey : metaArrayTable.get(key).keySet()) {
				ArrayList<Integer> list = metaArrayTable.get(key).get(innerKey);
				Utils.SOP(innerKey + ": ");
				for (Integer value : list)
					Utils.SOP(" " + value);
				Utils.SOPln("");
			}
		}
	}

	/**
	 * 
	 * @author - SOHAM
	 */
	public static String colors[] = { "blue1", "darkorchid1", "chocolate", "darkgoldenrod3", "mediumslateblue",
			"firebrick3", "gray73", "greenyellow", "white" };
	public static String zero_color = "ZERO_COLOR";

	public static int getRegisterForColor(String color) {
		if(color.equals(zero_color))
			return 0;

		for (int i = 0; i < colors.length; i++)
			if (colors[i].equals(color))
				return i+1;
		return colors.length+1;
	}

	public static void registerAllocation() throws Exception {
		LinkedHashMap<Integer, HashSet<Integer>> sortedinterfearanceGraph = sort();
		Utils.SOPln("");

		color_mapping = new LinkedHashMap<Integer, ArrayList<Boolean>>(/*sortedinterfearanceGraph.size()*/);
		for (Integer i : sortedinterfearanceGraph.keySet()) {
			
			/*Utils.SOP("Adding "+i + " to colormapping");
			Utils.SOPln(sortedinterfearanceGraph.get(i));*/
			color_mapping.put(i, new ArrayList<Boolean>(Collections.nCopies(colors.length, Boolean.TRUE)));
		}

		Utils.SOPln("");

		for (Integer i : color_mapping.keySet()) {
			if (isColored(i)) {
				/* Already Colored cause of Phi Cluster */
				/*Utils.SOPln("Instruction " + i + " Is already colored");*/
				continue;
			}

			int j = findNextAvailableColor(i);
			Instruction.getInstructionList().get(i).setColor(colors[j]);
			/*Utils.SOPln("Instruction " + i + " Color " + colors[j]);*/
			/* Do not follow-up if the color assigned is white = MEMORY */
			if (j != colors.length - 1) {
				followup(i, j);
				ArrayList<Integer> visited = new ArrayList<Integer>();
				if (phiClusters.containsKey(i)) {
					visited.add(i);
					for(int neighBor:phiClusters.get(i))
						colorCluster(neighBor, j, visited);
					visited.clear();
				}
			}
		}

		for (Integer i : phiClusters.keySet()) {
			Instruction phi = Instruction.getInstructionList().get(i);
			if (phi.getCode() != CODE.phi)
				continue;

			if (!phi.getColor().equals(phi.getLeftInstruction().getColor()))
				handlePhiMissColor(phi, phi.getLeftInstruction(), true);

			if (!phi.getColor().equals(phi.getRightInstruction().getColor()))
				handlePhiMissColor(phi, phi.getRightInstruction(), false);
		}
	}

	private static void handlePhiMissColor(Instruction phi, Instruction operand, boolean isLeftOp) {
		BasicBlock parent = null;

		if (isLeftOp)
			parent = phi.getMyBasicBlock().getFirstParent();
		else
			parent = phi.getMyBasicBlock().getSecondParent();
		Instruction i = Instruction.getInstruction(CODE.move, operand, phi, false).setBasicBlock(parent)
				.setColor(phi.getColor());
		Utils.SOPln("Adding Extra Move !! " + i+"  in BB "+parent.getIndex());
		parent.addLastInstruction(i);
	}

	private static boolean isColored(int i) {
		if (Instruction.getInstructionList().get(i).getColor() != ""
				&& !Instruction.getInstructionList().get(i).getColor().equals(colors[8]))
			return true;

		return false;
	}

	private static void colorCluster(int k, int color, ArrayList<Integer> visited) {
		/* SOPln("Asking to color "+k+" with "+color); */
		if (isColored(k) || visited.contains(k)) {
			return;
		}

		visited.add(k);
		if (isColorable(k, color)) {
			/* SOPln("Asking to color "+k+" with "+color+" succesfull"); */
			Instruction.getInstructionList().get(k).setColor(colors[color]);
			followup(k, color);
			/* Utils.SOPln("Instruction " + k + " Color " + color); */
		}

		for (Integer l : phiClusters.get(k)) {
			colorCluster(l, color, visited);
		}
	}

	private static boolean isColorable(int k, int color) {
		/*Utils.SOPln(color + " " + index);
		Utils.SOPln(k);
		Utils.SOPln(color_mapping.get(k));*/
		return color_mapping.get(k).get(color);
	}

	private static void followup(int i, int j) {
		for (Integer k : interfearanceGraph.get(i)) {
			color_mapping.get(k).set(j, false);
		}
	}

	public static void printColorMapping() {
		for (Integer i : color_mapping.keySet()) {
			Utils.SOP(i + ": ");
			for (boolean j : color_mapping.get(i))
				Utils.SOP(j + " ");
			Utils.SOPln("");
		}
	}

	public static int findNextAvailableColor(int i) {
		for (int j = 0; j < color_mapping.get(i).size(); j++) {
			if (color_mapping.get(i).get(j))
				return j;
		}
		return -1;
	}

	public static LinkedHashMap<Integer, HashSet<Integer>> sort() {
		List<Map.Entry<Integer, HashSet<Integer>>> aList = new ArrayList<Map.Entry<Integer, HashSet<Integer>>>(
				interfearanceGraph.entrySet());

		Collections.sort(aList, new Comparator<Map.Entry<Integer, HashSet<Integer>>>() {
			@Override
			public int compare(Entry<Integer, HashSet<Integer>> a, Entry<Integer, HashSet<Integer>> b) {
				int bDepth = Instruction.getInstructionList().get(b.getKey()).getWhileDepth();
				int aDepth = Instruction.getInstructionList().get(a.getKey()).getWhileDepth();
				if (bDepth == aDepth)
					return b.getValue().size() - a.getValue().size();
				return bDepth - aDepth;
			}
		});

		LinkedHashMap<Integer, HashSet<Integer>> interfearanceGraph1 = new LinkedHashMap<Integer, HashSet<Integer>>();
		for (Map.Entry<Integer, HashSet<Integer>> entry : aList) {
			interfearanceGraph1.put(entry.getKey(), entry.getValue());
		}
		return interfearanceGraph1;
	}

	public static void addClusterEdge(int phiIndex, int phiparent) {
		HashSet<Integer> parentedgeList = null;
		if (!phiClusters.containsKey(phiIndex)) {
			parentedgeList = new HashSet<Integer>();
			phiClusters.put(phiIndex, parentedgeList);
		} else
			parentedgeList = phiClusters.get(phiIndex);
		parentedgeList.add(phiparent);
		// parentedgeList.remove(phiIndex);

		parentedgeList = null;
		if (!phiClusters.containsKey(phiparent)) {
			parentedgeList = new HashSet<Integer>();
			phiClusters.put(phiparent, parentedgeList);
		} else
			parentedgeList = phiClusters.get(phiparent);
		parentedgeList.add(phiIndex);
	}

	public static void printPhiClusters() {
		for (Integer key : phiClusters.keySet()) {
			Utils.SOP("Phi ins " + key);
			HashSet<Integer> edges = phiClusters.get(key);
			for (int j : edges)
				Utils.SOP(" " + j);
			Utils.SOPln("");
		}
	}

	public static HashMap<Integer, HashSet<Integer>> getFixUpMap() {
		if (fixUpMap == null)
			fixUpMap = new HashMap<Integer, HashSet<Integer>>();
		return fixUpMap;
	}

	public static HashMap<Integer, HashSet<Integer>> getPhiClusters() {
		return phiClusters;
	}
	public static void traversefunc(BasicBlock current, HashSet<Integer> live) throws Exception {

		if (current.isVisited()) {
			/* Will be executed for LOOP_HEADER for 2nd time */
			current.visitAndUpdateLiveRange(live);
			current.getLeftOver().addAll(current.getLiveRange());
			current.getLeftOver().removeAll(current.getSecondChild().getLiveRange());
			Utils.SOPln("BB = " + current.getIndex() + " left Over = " + current.getLeftOver());
			for (int i : current.getLeftOver()) {
				if (!leftOverTrace.containsKey(i))
					leftOverTrace.put(i, current);
				else {
					/* 2nd Consecutive BB found, time to fix it */
					//SOPln("We have My case for " + i+" for BB = "+current.getIndex());
					ArrayList<BasicBlock> stop = new ArrayList<BasicBlock>();
					stop.add(current);
					leftOverTrace.get(i).getSecondChild().fixLeftOverTrace(stop, i);
					stop.clear();
					stop = null;
					leftOverTrace.put(i, current);
				}
			}

			return;
		}

		boolean shouldIVisit = false;
		String currentType = current.getTagtype();
		switch (currentType) {
		case "LOOP_HEADER":
			shouldIVisit = true;
			break;
		case "IF_HEADER":
			shouldIVisit = current.areBothChildrenVisited();
			break;

		case "REGULAR":
			shouldIVisit = true;
			break;
		}

		if (!shouldIVisit) {
			/* This is IF_HEADER for 1st time */
			current.addLiveRange(live);
			current.addLeftOver(current.getSecondChild().getLeftOver());
			return;
		} else if (currentType.equals("IF_HEADER"))
			current.addLeftOver(current.getFirstChild().getLeftOver());

		current.visitAndUpdateLiveRange(live);
		Utils.SOPln(current.getIndex());

		if (current.secondParentExists()) {
			HashSet<Integer> tobeGivenAhead = new HashSet<Integer>(current.getLiveRange());
			HashSet<Integer> weShallAddThis = new HashSet<Integer>();
			for (int abc = current.getInstructionList().size() - 1; abc >= 0; abc--) {
				Instruction ins = current.getInstructionList().get(abc);
				if (ins.getCode() != CODE.phi)
					continue;

				if (!current.isDeadInstruction(ins)) {
					tobeGivenAhead.remove((Integer) ins.getInstructionNumber());
					addEdge(ins.getInstructionNumber(), tobeGivenAhead);
					if (ins.getLeftInstruction() == null) {
						Utils.SOPln("Doing New Stuff");
						Result X = new Result();
						X.addressIfVariable = ins.getPhiFor();
						X.kind = RESULT_KIND.VAR;
						specialLoad(X);
						X.instruction.setBasicBlock(current.getFirstParent());
						current.getFirstParent().getInstructionList().add(X.instruction);
						ins.setLeftInstruction(X.instruction);
					}
					addClusterEdge(ins.getInstructionNumber(), ins.getLeftInstruction().getInstructionNumber());
					addClusterEdge(ins.getInstructionNumber(), ins.getRightInstruction().getInstructionNumber());
					weShallAddThis.add(ins.getRightInstruction().getInstructionNumber());
					leftOverTrace.remove((Integer) ins.getInstructionNumber());
				} else {
					Utils.SOPln(
							"Removing Instruction " + ins.getInstructionNumber() + " from BB " + current.getIndex());
					current.getInstructionList().remove(ins);
				}

			}
			tobeGivenAhead.addAll(weShallAddThis);
			current.getSecondParent().addLeftOver(current.getLeftOver());
			traversefunc(current.getSecondParent(), tobeGivenAhead);
		}

		if (current.firstParentExists()) {
			HashSet<Integer> tobeGivenAhead = new HashSet<Integer>(current.getLiveRange());
			HashSet<Integer> weShallAddThis = new HashSet<Integer>();
			for (int abc = current.getInstructionList().size() - 1; abc >= 0; abc--) {
				Instruction ins = current.getInstructionList().get(abc);

				if (ins.getCode() != CODE.phi)
					continue;

				if (!current.isDeadInstruction(ins)) {
					tobeGivenAhead.remove((Integer) ins.getInstructionNumber());
					addEdge(ins.getInstructionNumber(), tobeGivenAhead);
					addClusterEdge(ins.getInstructionNumber(), ins.getLeftInstruction().getInstructionNumber());
					addClusterEdge(ins.getInstructionNumber(), ins.getRightInstruction().getInstructionNumber());

					leftOverTrace.remove((Integer) ins.getInstructionNumber());
					if (ins.getLeftInstruction() == null)
						SOPln("CHECK " + ins.getInstructionNumber());
					weShallAddThis.add(ins.getLeftInstruction().getInstructionNumber());
				} else {
					Utils.SOPln(
							"Removing Instruction " + ins.getInstructionNumber() + " from BB " + current.getIndex());
					current.getInstructionList().remove(ins);
				}
			}

			tobeGivenAhead.addAll(weShallAddThis);
			current.getFirstParent().addLeftOver(current.getLeftOver());
			traversefunc(current.getFirstParent(), tobeGivenAhead);
		}
	}

	public static void startLowering(BasicBlock current) throws Exception {
		if (current.isbVisited() || current.isIgnored())
			return;

		boolean shouldIVisist = false;

		switch (current.getBlockType()) {
		case LOOP_HEADER:
		case IF_HEADER:
		case THEN_BLOCK:
		case ELSE_BLOCK:
		case DO_BLOCK:
		case LOOP_FOLLOW:
		case REGULAR:
			shouldIVisist = true;
			break;
		case FOLLOW_BLOCK:
			shouldIVisist = current.areBothParentVisited();
			break;
		}

		if (!shouldIVisist)
			return;

		// call convert2Machine
		current.lowerToMachineCode();
		//Utils.SOPln(current);

		if (current.firstChildExists())
			startLowering(current.getFirstChild());

		if (current.secondChildExists())
			startLowering(current.getSecondChild());
	}
	
	////////////////////////////////////////////////////////////////////////////
	public static void createTraceEdges(int index, ArrayList<Instruction> ins) {
		HashSet<Integer> edgeList = null;
		if (!interfearanceGraph.containsKey(index)) {
			edgeList = new HashSet<Integer>();
			interfearanceGraph.put(index, edgeList);
		} else
			edgeList = interfearanceGraph.get(index);

		for (Instruction i : ins) {
			int index1 = i.getInstructionNumber();
			if (index1 == index)
				continue;

			if (!interfearanceGraph.containsKey(index1))
				continue;

			interfearanceGraph.get(index1).add(index);
			edgeList.add(index1);
		}
	}

	public static void addEdge(int index1, HashSet<Integer> values) {
		HashSet<Integer> edgeList = null;
		if (!interfearanceGraph.containsKey(index1)) {
			edgeList = new HashSet<Integer>();
			interfearanceGraph.put(index1, edgeList);
		} else
			edgeList = interfearanceGraph.get(index1);
		edgeList.addAll(values);
		edgeList.remove(index1);

		for (int i : values) {
			if (i == index1)
				continue;

			edgeList = null;
			if (!interfearanceGraph.containsKey(i)) {
				edgeList = new HashSet<Integer>();
				interfearanceGraph.put(i, edgeList);
			} else
				edgeList = interfearanceGraph.get(i);
			edgeList.add(index1);
		}
	}
	
	public static HashMap<Integer, HashSet<Integer>> getInterfearenceGraph() {
		return interfearanceGraph;
	}

	public static void shutDown() {
		if (leftOverTrace != null)
			leftOverTrace.clear();
		leftOverTrace = null;

		if (metaStackAndFramePointerData != null)
			metaStackAndFramePointerData.clear();
		metaStackAndFramePointerData = null;

		if (metaIDTable != null)
			metaIDTable.clear();
		metaIDTable = null;

		if (metaArrayTable != null)
			metaArrayTable.clear();
		metaArrayTable = null;

		if (interfearanceGraph != null)
			interfearanceGraph.clear();
		interfearanceGraph = null;

		if (funcInfoTable != null)
			funcInfoTable.clear();
		funcInfoTable = null;

		if (functionList != null)
			functionList.clear();
		functionList = null;

		if (color_mapping != null)
			color_mapping.clear();
		color_mapping = null;
		
		if (phiClusters != null)
			phiClusters.clear();
		phiClusters = null;

		if (fixUpMap != null)
			fixUpMap.clear();
		fixUpMap = null;

		Instruction.shutDown();
		BasicBlock.shutDown();
	}
}