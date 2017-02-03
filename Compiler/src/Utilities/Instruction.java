package Utilities;

import Utilities.Utils.CODE;

public class Instruction {
	int a, b, c;
	CODE code;
	Instruction previousInAnchor;

	@Override
	public String toString() {
		return code.toString() + " " + a + " " + b + " " + c;
	}
}
