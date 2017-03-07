package Utilities;

import java.util.ArrayList;
import java.util.HashMap;

import Utilities.Utils.CODE;

public class Instruction {
	private ArrayList<Instruction> funcParameters = null;
	private boolean isLoadForArray = false;
	private static boolean allowNextAnchorTest = true;
	private static ArrayList<Instruction> mInstructionList;
	private Instruction aInstruction, bInstruction, previousInAnchor, referenceInstruction = null;
	private String aConstant, bConstant, phiFor = null, storeFor = null, aInsFor = null, bInsFor = null;
	private int index = -1;
	private CODE code;
	private BasicBlock myBasicBlock;
	private boolean isArray = false;

	private Instruction(CODE c, String a1, String b1, Instruction a2, Instruction b2, boolean addAuto) {
		this(c, a1, b1, a2, b2, addAuto, false);
	}

	private Instruction(CODE c, String a1, String b1, Instruction a2, Instruction b2, boolean addAuto, boolean isArr) {
		code = c;
		aInstruction = a2;
		bInstruction = b2;
		aConstant = a1;
		bConstant = b1;
		isArray = isArr;
		if (code == CODE.adda)
			isArray = true;

		nullCheck();
		index = mInstructionList.size();
		mInstructionList.add(this);

		if (!addAuto)
			return;

		myBasicBlock = BasicBlock.getCurrentBasicBlock();
		if (Utils.COPY_PROP)
			lastAccessTest();

		if (allowNextAnchorTest && Utils.COM_SUBEX_ELIM && !hasReferenceInstruction())
			anchorTest();

		allowNextAnchorTest = true;
		if (!hasReferenceInstruction()) {
			if (code == CODE.adda)
				allowNextAnchorTest = false;
			myBasicBlock.addInstruction(this, true);
		}
	}

	public static Instruction getInstruction(CODE c) {
		return getInstruction(c, true);
	}

	public static Instruction getInstruction(CODE c, boolean addAuto) {
		Instruction i = new Instruction(c, null, null, null, null, addAuto);
		if (i.referenceInstruction != null)
			return i.referenceInstruction;
		return i;
	}

	public static Instruction getInstruction(CODE c, String a1, String b1, boolean addAuto) {
		Instruction i = new Instruction(c, a1, b1, null, null, addAuto);
		if (i.referenceInstruction != null)
			return i.referenceInstruction;
		return i;
	}

	public static Instruction getInstruction(CODE c, String a1, String b1) {
		return getInstruction(c, a1, b1, true);
	}

	public static Instruction getInstruction(CODE c, String a1, Instruction b2) {
		Instruction i = new Instruction(c, a1, null, null, b2, true);
		if (i.referenceInstruction != null)
			return i.referenceInstruction;
		return i;
	}

	public static Instruction getInstruction(CODE c, Instruction a2, String b1) {
		Instruction i = new Instruction(c, null, b1, a2, null, true);
		if (i.referenceInstruction != null)
			return i.referenceInstruction;
		return i;
	}

	public static Instruction getInstruction(CODE c, Instruction a1, Instruction b1, boolean autoAdd) {
		Instruction i = new Instruction(c, null, null, a1, b1, autoAdd);
		if (i.referenceInstruction != null)
			return i.referenceInstruction;
		return i;
	}

	public static Instruction getInstructionForArray(CODE c, Instruction a1, Instruction b1) {
		return new Instruction(c, null, null, a1, b1, true, true);
	}

	public static Instruction getInstruction(CODE c, Instruction a1, Instruction b1) {
		return getInstruction(c, a1, b1, true);
	}

	public static void nullCheck() {
		if (mInstructionList == null)
			mInstructionList = new ArrayList<Instruction>();
	}

	public String toStringImpl() {
		/*
		 * if (referenceInstruction != null) return
		 * referenceInstruction.toStringImpl();
		 */
		return "" + index;
	}

	public static String toStringConstant(String constant, String funcName) {
		if (constant == null)
			return "null";

		if (constant.charAt(0) != '&')
			return constant;

		if (Integer.parseInt(constant.substring(1)) == Integer.MAX_VALUE)
			return "null";

		try {
			return "&" + Utils.address2Identifier(Integer.parseInt(constant.substring(1)), funcName);
		} catch (Exception E) {
			Utils.SOPln(E);
			return null;
		}
	}

	public String toStringConstant(String constant) {
		String ans = toStringConstant(constant, myBasicBlock.getFunctionName());
		if (ans == null)
			Utils.SOPln("WE GFOUND AN EXCEPTION FOR " + index);
		return ans;
	}

	public String testToString() {

		return index + ": " + code.toString() + " ("
				+ (aInstruction != null ? "" + aInstruction.toStringImpl() : "null") + ") ("
				+ (bInstruction != null ? "" + bInstruction.toStringImpl() : "null") + ") "
				+ toStringConstant(aConstant) + " " + toStringConstant(bConstant)
				+ (phiFor != null ? "  PHI FOR " + toStringConstant(phiFor) : "")
				+ (storeFor == null ? "" : " STORE FOR= " + toStringConstant(storeFor))
				+ (referenceInstruction != null ? " REPLACED WITH (" + referenceInstruction.toStringImpl() + ")" : "");
	}

	private boolean shouldPrint(String toPrint) {
		if (toPrint == null)
			return false;

		if (toPrint.equals("&" + Integer.MAX_VALUE))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (hasReferenceInstruction())
			return referenceInstruction.toString();

		return index + ": " + code.toString() + " ("
				+ (aInstruction != null ? "" + aInstruction.toStringImpl() : "null") + ") ("
				+ (bInstruction != null ? "" + bInstruction.toStringImpl() : "null") + ") "
				+ toStringConstant(aConstant) + " " + toStringConstant(bConstant)
				+ (shouldPrint(aInsFor) ? " aFor = " + toStringConstant(aInsFor) : "")
				+ (shouldPrint(bInsFor) ? " bFor = " + toStringConstant(bInsFor) : "")
				+ (phiFor != null ? "  PHI FOR " + toStringConstant(phiFor) : "")
				+ (storeFor == null ? "" : " STORE FOR= " + toStringConstant(storeFor))
				+ (hasFunctionParameters() ? " has func params" : "");
	}

	public boolean hasReferenceInstruction() {
		return (referenceInstruction != null);
	}

	public boolean hasFunctionParameters() {
		return funcParameters != null;
	}

	public Instruction setFunctionParameters(ArrayList<Result> R) {
		if (R == null)
			return this;

		funcParameters = new ArrayList<Instruction>();
		for (Result r : R)
			funcParameters.add(r.instruction);

		return this;
	}

	public Instruction setBasicBlock(BasicBlock bb) {
		myBasicBlock = bb;
		return this;
	}

	public void setRightInstruction(Instruction i) {
		bInstruction = i;
	}

	public void setLeftInstruction(Instruction i) {
		aInstruction = i;
	}

	public Instruction getRightInstruction() {
		return bInstruction;
	}

	public Instruction getLeftInstruction() {
		return aInstruction;
	}

	public String getRightConstant() {
		return bConstant;
	}

	public void setReferenceInstruction(Instruction ref) {
		referenceInstruction = ref;
	}

	public int getInstructionNumber() {
		return index;
	}

	public static ArrayList<Instruction> getInstructionList() {
		return mInstructionList;
	}

	public Instruction getReferenceInstruction() {
		return referenceInstruction;
	}

	public static Instruction getCurrentInstruction() {
		return mInstructionList.get(mInstructionList.size() - 1);
	}

	public void clearReferenceInstruction() {
		referenceInstruction = null;
	}

	public String getAInsFor() {
		return aInsFor;
	}

	public String getBInsFor() {
		return bInsFor;
	}

	public Instruction setAInsFor(String abc) {
		aInsFor = abc;
		return this;
	}

	public Instruction setBInsFor(String abc) {
		bInsFor = abc;
		return this;
	}

	public Instruction setStoreFor(String sFor) {
		storeFor = sFor;
		return this;
	}

	public Instruction setPhiFor(String str) {
		phiFor = str;
		return this;
	}

	public String getPhiFor() {
		return phiFor;
	}

	public CODE getCode() {
		return code;
	}

	public boolean isLoadForArray() {
		return isLoadForArray;
	}

	public Instruction setLoadForArray() {
		isLoadForArray = true;
		return this;
	}

	public void setPreviousInAnchor(Instruction prev) {
		previousInAnchor = prev;
	}

	private boolean isBEqual(Instruction i) {
		if (bInstruction != null && i.bInstruction != null)
			return (bInstruction == i.bInstruction || bInstruction.isDuplicate(i.bInstruction));

		if (bConstant != null && i.bConstant != null)
			return bConstant.equals(i.bConstant);

		return false;
	}

	private boolean isAEqual(Instruction i) {
		if (aInstruction != null && i.aInstruction != null)
			return (aInstruction == i.aInstruction || aInstruction.isDuplicate(i.aInstruction));

		if (aConstant != null && i.aConstant != null)
			return aConstant.equals(i.aConstant);

		return false;
	}

	public boolean isDuplicate(Instruction i) {
		return (code != CODE.phi && code == i.code) && isAEqual(i) && (isArray && code == CODE.load ? true : isBEqual(i));
	}

	private void lastAccessTest() {

		if (code == CODE.move) {
			referenceInstruction = bInstruction;
			myBasicBlock.updateLastAccessFor(aConstant, bInstruction);
		} else if (code == CODE.load) {
			referenceInstruction = myBasicBlock.getLastAccessFor(bConstant);
			if (!hasReferenceInstruction())
				myBasicBlock.updateLastAccessFor(bConstant, this);
		}
	}

	public void anchorTest(HashMap<CODE, Instruction> anchor) {
		if (/* !specialPermission && */Utils.doNotTestAnchor.contains(code))
			return;

		if (anchor == null)
			anchor = myBasicBlock.getAnchor();

		Instruction anchorTest = anchor.get(code);
		while (anchorTest != null) {
			if (!isDuplicate(anchorTest)) {
				anchorTest = anchorTest.previousInAnchor;
			} else {
				referenceInstruction = anchorTest;
				break;
			}
		}
	}

	private void anchorTest() {
		anchorTest(null);
	}
}