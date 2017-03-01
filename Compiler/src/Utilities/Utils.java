package Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import Utilities.MyScanner.CLASS;

public class Utils {
	public static int WHILE_DEPTH = 0;
	public static final boolean COM_SUBEX_ELIM = true;
	public static final boolean COPY_PROP = true;
	public static List<CODE> doNotTestAnchor = Arrays.asList(CODE.CMP, CODE.CMPI, CODE.BSR, CODE.BEQ, CODE.BNE,
			CODE.BLT, CODE.BGE, CODE.BLE, CODE.BGT, CODE.store);
	private static ArrayList<String> idTable = null;
	private static HashMap<Integer, ArrayList<Integer>> arrayInfoTable = null;
	private static HashMap<Integer, Integer> funcInfoTable = null;

	public static void nullCheck() {
		if (idTable == null)
			idTable = new ArrayList<String>();

		if (arrayInfoTable == null)
			arrayInfoTable = new HashMap<Integer, ArrayList<Integer>>();

		if (funcInfoTable == null)
			funcInfoTable = new HashMap<Integer, Integer>();
	}

	public static String address2Identifier(int id) throws Exception {
		nullCheck();

		if (id >= idTable.size() || id < 0)
			Utils.error("ID " + id + " NOT FOUND");

		return idTable.get(id);
	}

	public static void updateParamSize(int id, int paramSize) {
		funcInfoTable.put(id, paramSize);
	}

	public static int identifier2Address(String name) {
		return identifier2Address(name, null, CLASS.NONE);
	}

	public static int identifier2Address(String name, ArrayList<Integer> arrayInfo, CLASS mClass) {
		nullCheck();

		if (!idTable.contains(name)) {
			idTable.add(name);
			Utils.SOPln(name + " = &" + (idTable.size() - 1));

			if (mClass == CLASS.ARR) {
				arrayInfoTable.put((idTable.size() - 1), arrayInfo);
			} else if (mClass == CLASS.FUNC) {
				funcInfoTable.put((idTable.size() - 1), 0);
			}

		}
		return idTable.indexOf(name);
	}

	public static void SOPln(Object toPrint) {
		SOP(toPrint.toString() + "\n");
	}

	public static void SOP(Object toPrint) {
		System.out.print(toPrint.toString());
	}

	public static enum RESULT_KIND {
		NONE, CONST, VAR, INSTRUCTION, CONDITION
	};

	public static enum CODE {
		NONE, ADD, SUB, MUL, DIV, MOD, CMP, OR, AND, BIC, XOR, LSH, ASH, CHK, ADDI, SUBI, MULI, DIVI, MODI, CMPI, ORI, ANDI, BICI, XORI, LSHI, ASHI, CHKI, LDW, LDX, POP, STW, STX, PSH, BEQ, BNE, BLT, BGE, BLE, BGT, BSR, JSR, RET, RDD, WRD, WRH, WRL, adda, move, store, load, phi
	};

	final public static boolean BINARY = false;
	public static int programCounter = 0;
	public static int stackPointer = 0;
	private static ArrayList<Integer> buffer = new ArrayList<Integer>();
	private static ArrayList<String> tempResult = new ArrayList<String>();

	/* TODO: need to fixup; later */
	public static void fixup(int index) {
		SOPln("We are fixing up for index " + index);
		/*
		 * Instruction i = Instruction.instructionList.get(index);
		 * i.fixup(Instruction.getCurrentInstructionIndex() + 1 - index);
		 * SOPln(i);
		 */
	}

	public static void fixupOld(int index) {
		int currentVal = buffer.remove(index);
		currentVal = currentVal & 0xffff0000 + (programCounter - index);
		buffer.add(index, currentVal);
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

	private static String format(String input, int length) {
		for (int i = input.length() + 1; i <= length; i++)
			input = "0" + input;
		return input;
	}

	private static void putF3(int code, int c, String operation) throws Exception {
		String op = Integer.toBinaryString(code);
		op = format(op, 6);
		String cbits = Integer.toBinaryString(c);
		if (cbits.length() > 26)
			error("Length of Cbits is greater than 26 !! ");

		cbits = format(cbits, 26);
		if (BINARY) {
			SOPln(op + "-" + cbits);
		} else {
			SOPln(operation + " " + c);
		}
		buffer.add(programCounter, Integer.parseInt(op + cbits));
		tempResult.add(programCounter++, operation + " c = " + c);
	}

	private static void putF2(int code, int a, int b, int c, String operation) throws Exception {
		if (a >= 32)
			error("Length of abits is greater than 5 !! ");

		if (b >= 32)
			error("Length of bbits is greater than 5 !! ");

		if (c >= 32)
			error("Length of cbits is greater than 5 !! ");
		implementF1F2(code, a, b, c, operation);
	}

	private static void implementF1F2(int code, int a, int b, int c, String operation) {
		String op = Integer.toBinaryString(code);
		op = format(op, 6);
		String abits = Integer.toBinaryString(a);
		abits = format(abits, 5);
		String bbits = Integer.toBinaryString(b);
		bbits = format(bbits, 5);
		String cbits = Integer.toBinaryString(c);
		cbits = format(cbits, 16);
		if (BINARY)
			SOPln(op + "-" + abits + "-" + bbits + "-" + cbits);
		else
			SOPln(operation + " " + a + " " + b + " " + c);

		// buffer.add(programCounter, Integer.parseInt(op + abits + bbits +
		// cbits));
		tempResult.add(programCounter++, operation + " a = " + a + " b = " + b + " c = " + c);
	}

	private static void putF1(int code, int a, int b, int c, String operation) throws Exception {
		if (a >= 32)
			error("Length of abits is greater than 5 !! ");

		if (b >= 32)
			error("Length of bbits is greater than 5 !! ");

		if (c >= 65536)
			error("Length of cbits is greater than 16 !! ");

		implementF1F2(code, a, b, c, operation);
	}

	public static void emit(String command) {
		SOPln(command);
	}

	public static void put(CODE op, int a, int b, int c) throws Exception {
		if (a < 0 || b < 0 || c < 0)
			error("Invalid Operand, can't have negative operands ");
		switch (op) {
		case CHK:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF2(14, a, b, c, op.toString());
			break;
		case ASH:
			putF2(13, a, b, c, op.toString());
			break;
		case LSH:
			putF2(12, a, b, c, op.toString());
			break;
		case XOR:
			putF2(11, a, b, c, op.toString());
			break;
		case BIC:
			putF2(10, a, b, c, op.toString());
			break;
		case AND:
			putF2(9, a, b, c, op.toString());
			break;
		case OR:
			putF2(8, a, b, c, op.toString());
			break;
		case CMP:
			putF2(5, a, b, c, op.toString());
			break;
		case MOD:
			putF2(4, a, b, c, op.toString());
			break;
		case DIV:
			putF2(3, a, b, c, op.toString());
			break;
		case MUL:
			putF2(2, a, b, c, op.toString());
			break;
		case SUB:
			putF2(1, a, b, c, op.toString());
			break;
		case ADD:
			putF2(0, a, b, c, op.toString());
			break;
		case CHKI:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF1(30, a, b, c, op.toString());
			break;
		case ASHI:
			putF1(29, a, b, c, op.toString());
			break;
		case LSHI:
			putF1(28, a, b, c, op.toString());
			break;
		case XORI:
			putF1(27, a, b, c, op.toString());
			break;
		case BICI:
			putF1(26, a, b, c, op.toString());
			break;
		case ANDI:
			putF1(25, a, b, c, op.toString());
			break;
		case ORI:
			putF1(24, a, b, c, op.toString());
			break;
		case CMPI:
			putF1(21, a, b, c, op.toString());
			break;
		case MODI:
			putF1(20, a, b, c, op.toString());
			break;
		case DIVI:
			putF1(19, a, b, c, op.toString());
			break;
		case MULI:
			putF1(18, a, b, c, op.toString());
			break;
		case SUBI:
			putF1(17, a, b, c, op.toString());
			break;
		case ADDI:
			putF1(16, a, b, c, op.toString());
			break;
		case LDW:
			putF1(32, a, b, c, op.toString());
			break;
		case LDX:
			putF1(33, a, b, c, op.toString());
			break;
		case POP:
			putF1(34, a, b, c, op.toString());
			break;
		case STW:
			putF1(36, a, b, c, op.toString());
			break;
		case STX:
			putF2(37, a, b, c, op.toString());
			break;
		case PSH:
			putF1(38, a, b, c, op.toString());
			break;
		case BEQ:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF1(40, a, 0, c, op.toString());
			break;
		case BNE:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF1(41, a, 0, c, op.toString());
			break;
		case BLT:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF1(42, a, 0, c, op.toString());
			break;
		case BGE:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF1(43, a, 0, c, op.toString());
			break;
		case BLE:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF1(44, a, 0, c, op.toString());
			break;
		case BGT:
			if (b != 0)
				error("Invalid Operand, can't give b  ");

			putF1(45, a, 0, c, op.toString());
			break;
		case BSR:
			if (a != 0 || b != 0)
				error("Invalid Operand, can't give a and b  ");

			putF1(46, 0, 0, c, op.toString());
			break;
		case JSR:
			if (a != 0 || b != 0)
				error("Invalid Operand, can't give a and b  ");

			putF3(48, c, op.toString());
			break;
		case RET:
			if (a != 0 || b != 0)
				error("Invalid Operand, can't give a and b  ");

			putF2(49, 0, 0, c, op.toString());
			break;
		case RDD:
			if (c != 0 || b != 0)
				error("Invalid Operand, can't give c and b  ");

			putF2(50, a, 0, 0, op.toString());
			break;
		case WRD:
			if (c != 0 || a != 0)
				error("Invalid Operand, can't give a and c  ");

			putF2(51, 0, b, 0, op.toString());
			break;
		case WRH:
			if (c != 0 || a != 0)
				error("Invalid Operand, can't give a and c  ");

			putF2(51, 0, b, 0, op.toString());
			break;
		case WRL:
			if (b != 0 || a != 0 || c != 0)
				error("Invalid Operand, can't give operands");

			putF1(53, 0, 0, 0, op.toString());
			break;
		default:
			error("Invalid Operand code ");
		}
	}

	private static boolean registers[] = new boolean[10];

	private static int allocateRegister() {
		for (int i = 1; i < registers.length; i++) {
			if (!registers[i]) {
				registers[i] = true;
				return i;
			}
		}
		return -1;
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
						handleCompare(opCode);

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
					X.addressIfVariable = -1;
					if (command == CODE.CMP)
						handleCompare(opCode);
				}
			}
		}
	}

	public static void handleCompare(int code) {
		switch (code) {
		case ScannerUtils.leqToken:
			Instruction.getInstruction(CODE.BGT);
			break;
		case ScannerUtils.neqToken:
			Instruction.getInstruction(CODE.BEQ);
			break;
		case ScannerUtils.eqlToken:
			Instruction.getInstruction(CODE.BNE);
			break;
		case ScannerUtils.geqToken:
			Instruction.getInstruction(CODE.BLT);
			break;
		case ScannerUtils.gtrToken:
			Instruction.getInstruction(CODE.BLE);
			break;
		case ScannerUtils.lssToken:
			Instruction.getInstruction(CODE.BGE);
			break;
		}
	}

	public static void deallocateRegister(int regNo) {
		registers[regNo] = false;
	}

	public static void load(Result X) throws Exception {
		if (X.kind == RESULT_KIND.CONST) {
			X.instruction = Instruction.getInstruction(CODE.ADDI, "#0", "#" + X.valueIfConstant);
		} else if (X.kind == RESULT_KIND.VAR && !X.isArray) {
			X.instruction = Instruction.getInstruction(CODE.load, "#30", "&" + X.addressIfVariable);
		} else if (X.kind == RESULT_KIND.VAR && X.isArray) {
			Result a = getOffsetForArray(X);
			Result b = new Result();
			b.kind = RESULT_KIND.CONST;
			b.valueIfConstant = 4;
			compute(ScannerUtils.plusToken, a, b);
			load(a);
			Instruction two = Instruction.getInstruction(CODE.ADDI, "#30", "&" + X.addressIfVariable);
			Instruction three = Instruction.getInstruction(CODE.adda, a.instruction, two);
			X.instruction = Instruction.getInstructionForArray(CODE.load, three, null);
		}
		X.kind = RESULT_KIND.INSTRUCTION;
	}

	public static void becomes(Result X, Result Y) throws Exception {

		load(Y);
		if (!X.isArray) {
			Instruction.getInstruction(CODE.move, "&" + X.addressIfVariable, Y.instruction)
					.setAInsFor("&" + X.addressIfVariable).setBInsFor("&" + Y.addressIfVariable);
		} else {

			Result a = getOffsetForArray(X);
			Result b = new Result();
			b.kind = RESULT_KIND.CONST;
			b.valueIfConstant = 4;
			compute(ScannerUtils.plusToken, a, b);
			load(a);
			Instruction two = Instruction.getInstruction(CODE.ADDI, "#30", "&" + X.addressIfVariable);
			Instruction three = Instruction.getInstruction(CODE.adda, a.instruction, two);
			Instruction.getInstructionForArray(CODE.store, Y.instruction, three).setStoreFor("&" + X.addressIfVariable);
		}
	}

	public static boolean isInLoop() {
		return WHILE_DEPTH > 0;
	}

	public static Result getOffsetForArray(Result X) throws Exception {
		ArrayList<Integer> indices = arrayInfoTable.get(X.addressIfVariable);
		if (X.arrayExp.size() != indices.size())
			Utils.error("Array index mismatch");

		Result temp = new Result();
		temp.valueIfConstant = 0;
		temp.kind = RESULT_KIND.CONST;

		for (int i = 0; i < X.arrayExp.size(); i++) {
			Result r = X.arrayExp.get(i);
			if (r.kind == RESULT_KIND.CONST && r.valueIfConstant >= indices.get(i))
				Utils.error("Array Index " + r.valueIfConstant + " out of range " + indices.get(i) + " at line "
						+ MyScanner.getLineCount());

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
		for (Integer key : arrayInfoTable.keySet()) {
			ArrayList<Integer> list = arrayInfoTable.get(key);
			Utils.SOP(key + ": ");
			for (Integer value : list)
				Utils.SOP(" " + value);
			Utils.SOPln("");
		}
	}
}