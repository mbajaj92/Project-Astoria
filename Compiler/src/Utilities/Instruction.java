package Utilities;

import Utilities.Utils.CODE;

public class Instruction {
	int aIns, bIns;
	int index = -1;
	String aCons, bCons;
	CODE code;
	Instruction previousInAnchor;

	private Instruction(CODE c, String a1, String b1, int a2, int b2) {
		code = c;
		aIns = a2;
		bIns = b2;
		aCons = a1;
		bCons = b1;
	}

	public static Instruction getInstruction(CODE c, String a1, String b1) {
		Instruction i = new Instruction(c, a1, b1, -1, -1);
		Utils.inslist.add(i);
		i.index = Utils.inslist.size()-1;
		System.out.println(i);
		return i;
	}

	public static Instruction getInstruction(CODE c, String a1, int b2) {
		Instruction i = new Instruction(c, a1, null, -1, b2);
		Utils.inslist.add(i);
		i.index = Utils.inslist.size()-1;
		System.out.println(i);
		return i;
	}

	public static Instruction getInstruction(CODE c, int a2, String b1) {
		Instruction i = new Instruction(c, null, b1, a2, -1);
		Utils.inslist.add(i);
		i.index = Utils.inslist.size()-1;
		System.out.println(i);
		return i;
	}

	public static Instruction getInstruction(CODE c, int a1, int b1) {
		Instruction i = new Instruction(c, null, null, a1, b1);
		Utils.inslist.add(i);
		i.index = Utils.inslist.size()-1;
		System.out.println(i);
		return i;
	}

	@Override
	public String toString() {
		return index+": "+code.toString() + " " + aIns + " " + bIns + " " + aCons + " " + bCons;
	}
}