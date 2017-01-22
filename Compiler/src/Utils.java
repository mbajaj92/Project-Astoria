import java.util.ArrayList;

public class Utils {

	final public static boolean BINARY = false;
	public static int programCounter = 0;
	public static int stackPointer = 0;
	private static ArrayList<Integer> buffer = new ArrayList<Integer>();

	static class Result {
		static enum KIND {
			CONST, VAR, REG, CONDITION
		};

		KIND kind;
		int value;
		int address;
		int regno;
		String cond;
		int fixuplocation;
	}

	public static void conditionalJump(Result X) throws Exception {
		put(negateCondition(X.cond), X.regno, 0, 0);
		X.fixuplocation = programCounter - 1;
	}

	public static void fixup(int index) {
		int currentVal = buffer.remove(index);
		currentVal = currentVal & 0xffff0000 + (programCounter - index);
		buffer.add(index, currentVal);
	}

	public static String negateCondition(String cond) throws Exception {
		switch (cond) {
		case "BEQ":
			return "BNE";
		case "BNE":
			return "BEQ";
		case "BLT":
			return "BGE";
		case "BGE":
			return "BLT";
		case "BLE":
			return "BGT";
		case "BGT":
			return "BLE";
		default:
			throw new Exception("Invalid Conditional Operator !");
		}
	}

	// public static void main(String args[]) throws Exception {

	// }

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
			throw new Exception("Length of Cbits is greater than 26 !! ");

		cbits = format(cbits, 26);
		if (BINARY) {
			System.out.println(op + "-" + cbits);
		} else {
			System.out.println(operation + " " + c);
		}
		buffer.add(programCounter++, Integer.parseInt(op + cbits));
	}

	private static void putF2(int code, int a, int b, int c, String operation) throws Exception {
		if (a >= 32)
			throw new Exception("Length of abits is greater than 5 !! ");

		if (b >= 32)
			throw new Exception("Length of bbits is greater than 5 !! ");

		if (c >= 32)
			throw new Exception("Length of cbits is greater than 5 !! ");
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
			System.out.println(op + "-" + abits + "-" + bbits + "-" + cbits);
		else
			System.out.println(operation + " " + a + " " + b + " " + c);

		buffer.add(programCounter++, Integer.parseInt(op + abits + bbits + cbits));
	}

	private static void putF1(int code, int a, int b, int c, String operation) throws Exception {
		if (a >= 32)
			throw new Exception("Length of abits is greater than 5 !! ");

		if (b >= 32)
			throw new Exception("Length of bbits is greater than 5 !! ");

		if (c >= 65536)
			throw new Exception("Length of cbits is greater than 16 !! ");

		implementF1F2(code, a, b, c, operation);
	}

	public static void put(String op, int a, int b, int c) throws Exception {
		if (a < 0 || b < 0 || c < 0)
			throw new Exception("Invalid Operand, can't have negative operands ");
		switch (op) {
		case "CHK":
			if (b != 0)
				throw new Exception("Invalid Operand, can't give b  ");

			putF2(14, a, b, c, op);
			break;
		case "ASH":
			putF2(13, a, b, c, op);
			break;
		case "LSH":
			putF2(12, a, b, c, op);
			break;
		case "XOR":
			putF2(11, a, b, c, op);
			break;
		case "BIC":
			putF2(10, a, b, c, op);
			break;
		case "AND":
			putF2(9, a, b, c, op);
			break;
		case "OR":
			putF2(8, a, b, c, op);
			break;
		case "CMP":
			putF2(5, a, b, c, op);
			break;
		case "MOD":
			putF2(4, a, b, c, op);
			break;
		case "DIV":
			putF2(3, a, b, c, op);
			break;
		case "MUL":
			putF2(2, a, b, c, op);
			break;
		case "SUB":
			putF2(1, a, b, c, op);
			break;
		case "ADD":
			putF2(0, a, b, c, op);
			break;
		case "CHKI":
			if (b != 0)
				throw new Exception("Invalid Operand, can't give b  ");

			putF1(30, a, b, c, op);
			break;
		case "ASHI":
			putF1(29, a, b, c, op);
			break;
		case "LSHI":
			putF1(28, a, b, c, op);
			break;
		case "XORI":
			putF1(27, a, b, c, op);
			break;
		case "BICI":
			putF1(26, a, b, c, op);
			break;
		case "ANDI":
			putF1(25, a, b, c, op);
			break;
		case "ORI":
			putF1(24, a, b, c, op);
			break;
		case "CMPI":
			putF1(21, a, b, c, op);
			break;
		case "MODI":
			putF1(20, a, b, c, op);
			break;
		case "DIVI":
			putF1(19, a, b, c, op);
			break;
		case "MULI":
			putF1(18, a, b, c, op);
			break;
		case "SUBI":
			putF1(17, a, b, c, op);
			break;
		case "ADDI":
			putF1(16, a, b, c, op);
			break;
		case "LDW":
			putF1(32, a, b, c, op);
			break;
		case "LDX":
			putF1(33, a, b, c, op);
			break;
		case "POP":
			putF1(34, a, b, c, op);
			break;
		case "STW":
			putF1(36, a, b, c, op);
			break;
		case "STX":
			putF2(37, a, b, c, op);
			break;
		case "PSH":
			putF1(38, a, b, c, op);
			break;
		case "BEQ":
			if (b != 0)
				throw new Exception("Invalid Operand, can't give b  ");

			putF1(40, a, 0, c, op);
			break;
		case "BNE":
			if (b != 0)
				throw new Exception("Invalid Operand, can't give b  ");

			putF1(41, a, 0, c, op);
			break;
		case "BLT":
			if (b != 0)
				throw new Exception("Invalid Operand, can't give b  ");

			putF1(42, a, 0, c, op);
			break;
		case "BGE":
			if (b != 0)
				throw new Exception("Invalid Operand, can't give b  ");

			putF1(43, a, 0, c, op);
			break;
		case "BLE":
			if (b != 0)
				throw new Exception("Invalid Operand, can't give b  ");

			putF1(44, a, 0, c, op);
			break;
		case "BGT":
			if (b != 0)
				throw new Exception("Invalid Operand, can't give b  ");

			putF1(45, a, 0, c, op);
			break;
		case "BSR":
			if (a != 0 || b != 0)
				throw new Exception("Invalid Operand, can't give a and b  ");

			putF1(46, 0, 0, c, op);
			break;
		case "JSR":
			if (a != 0 || b != 0)
				throw new Exception("Invalid Operand, can't give a and b  ");

			putF3(48, c, op);
			break;
		case "RET":
			if (a != 0 || b != 0)
				throw new Exception("Invalid Operand, can't give a and b  ");

			putF2(49, 0, 0, c, op);
			break;
		case "RDD":
			if (c != 0 || b != 0)
				throw new Exception("Invalid Operand, can't give c and b  ");

			putF2(50, a, 0, 0, op);
			break;
		case "WRD":
			if (c != 0 || a != 0)
				throw new Exception("Invalid Operand, can't give a and c  ");

			putF2(51, 0, b, 0, op);
			break;
		case "WRH":
			if (c != 0 || a != 0)
				throw new Exception("Invalid Operand, can't give a and c  ");

			putF2(51, 0, b, 0, op);
			break;
		case "WRL":
			if (b != 0 || a != 0 || c != 0)
				throw new Exception("Invalid Operand, can't give operands");

			putF1(53, 0, 0, 0, op);
			break;
		default:
			throw new Exception("Invalid Operand code ");
		}
	}
}
