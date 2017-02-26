package Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import Utilities.Utils.CODE;

public class BasicBlock {
	private static ArrayList<BasicBlock> fixList;
	private String TAG, mFunctionName;
	private boolean ignore = false;
	private static ArrayList<BasicBlock> mBasicBlockList;
	private int index = -1;
	private boolean oneChildON = false, twoChildON = false, oneParentON = false, twoParentON = false;
	private BasicBlock oneChild, twoChild, oneParent, twoParent;
	private ArrayList<Instruction> mInstructionSet = null;
	private List<CODE> toBeFixed = Arrays.asList(CODE.BGE,CODE.BEQ,CODE.BGT,CODE.BLE,CODE.BLT,CODE.BNE, CODE.BSR);
	private static List<CODE> notToBeAnchored = Arrays.asList(CODE.phi, CODE.BEQ, CODE.BGE, CODE.BLE, CODE.BGT,
			CODE.BLT, CODE.BNE);
	private HashMap<CODE,Instruction> anchor = null;
	private HashMap<String,Instruction> lastAccessTable = null;

	BasicBlock(String tag) {
		this(tag, null);
	}

	BasicBlock(String tag, String name) {
		TAG = tag;
		mFunctionName = name;
		nullCheck();
		mInstructionSet = new ArrayList<Instruction>();
		anchor = new HashMap<CODE, Instruction>();
		lastAccessTable = new HashMap<String, Instruction>();
		mBasicBlockList.add(this);
		index = mBasicBlockList.size() - 1;

	}

	public Instruction getAnchorInstructionForCode(CODE code) {
		if (anchor.containsKey(code))
			return anchor.get(code);
		return null;
	}

	public void addInstruction(Instruction i) {
		CODE code = i.getCode();
		if (!notToBeAnchored.contains(code)) {

			if (anchor.containsKey(code))
				i.setPreviousInAnchor(anchor.get(code));

			if(code == CODE.store)
				anchor.remove(CODE.adda);

			anchor.put(code, i);
		}

		if (code == CODE.phi)
			mInstructionSet.add(0, i);
		else
			mInstructionSet.add(i);
	}

	public void setAsCurrent() {
		mBasicBlockList.add(this);
		index = mBasicBlockList.size()-1;
	}

	public HashMap<String, Instruction> getLastAccessTable() {
		return lastAccessTable;
	}

	public static BasicBlock getCurrentBasicBlock() {
		return mBasicBlockList.get(mBasicBlockList.size() - 1);
	}

	public void copyParentLastAccess(BasicBlock parent) {
		lastAccessTable.putAll(parent.lastAccessTable);
	}

	private void takeCareOfPhi(HashMap<String, Instruction> leftTable, HashMap<String, Instruction> rightTable,
			boolean isWhile) throws Exception {
		for (String key : leftTable.keySet()) {
			Instruction left = leftTable.get(key);
			if (left == null)
				Utils.error("Left Chain is null for "+Instruction.toStringConstant(key));
			Instruction right = rightTable.get(key);
			if (right == null)
				Utils.error("Right Chain is null for "+Instruction.toStringConstant(key));

			if (left != right) {
				if (isWhile) {
					Instruction i = Instruction.getInstruction(CODE.phi, left, right, false).setPhiFor(key);
					mInstructionSet.add(0, i);
					updateLastAccessFor(key, i);
				} else
					updateLastAccessFor(key, Instruction.getInstruction(CODE.phi, left, right).setPhiFor(key));
			} else
				updateLastAccessFor(key, left);
		}

		for (String key : rightTable.keySet()) {
			Instruction left = leftTable.get(key);
			if (left == null)
				Utils.error("Left Chain is null for for "+Instruction.toStringConstant(key));
			Instruction right = rightTable.get(key);
			if (right == null)
				Utils.error("Right Chain is null for for "+Instruction.toStringConstant(key));

			if (left != right && !lastAccessTable.containsKey(key)) {
				if (isWhile) {
					Instruction i = Instruction.getInstruction(CODE.phi, left, right, false).setPhiFor(key);
					mInstructionSet.add(0, i);
					updateLastAccessFor(key, i);
				} else
					updateLastAccessFor(key, Instruction.getInstruction(CODE.phi, left, right).setPhiFor(key));
			} else if (!lastAccessTable.containsKey(key))
				updateLastAccessFor(key, right);
		}
	}

	private void takeCareOfAccess(BasicBlock parent) {
		if (mInstructionSet.isEmpty()) {
			lastAccessTable.putAll(parent.lastAccessTable);
			return;
		}

		for (Instruction i : mInstructionSet) {
			if (i.getPhiFor() != null)
				lastAccessTable.put(i.getPhiFor(), i);
		}

		for (String key : parent.lastAccessTable.keySet()) {
			if (lastAccessTable.containsKey(key))
				continue;
			lastAccessTable.put(key, parent.lastAccessTable.get(key));
		}
	}

	public void updateAnchorLastAccessAndPhi(BasicBlock parent) throws Exception {
		updateAnchorLastAccessAndPhi(parent, null, null);
	}

	public void generetePhiAndUpdateTree(HashMap<String, Instruction> leftTable, HashMap<String, Instruction> rightTable) throws Exception {
		takeCareOfPhi(leftTable, rightTable, true);
		fixPhis();
	}

	public void updateAnchorLastAccessAndPhi(BasicBlock parent, HashMap<String, Instruction> leftParent,
			HashMap<String, Instruction> rightParent) throws Exception {
		anchor.putAll(parent.anchor);

		if (leftParent != null && rightParent != null)
			takeCareOfPhi(leftParent, rightParent, false);
		takeCareOfAccess(parent);
	}

	public void deleteLastInstruction() {
		mInstructionSet.remove(mInstructionSet.size() - 1);
	}

	public void setParent(BasicBlock parent) throws Exception {
		mFunctionName = parent.getFunctionName();
		if (!oneParentON) {
			oneParent = parent;
			oneParentON = true;
		} else if (!twoParentON) {
			twoParent = parent;
			twoParentON = true;
		} else
			Utils.error("Adding 3rd Parent is not allowed");
	}
	
	public void setChild(BasicBlock child, boolean sameChildInDominotorAndCFG) throws Exception {
		if (!oneChildON) {
			oneChild = child;
			oneChildON = true;
		} else if (!twoChildON) {
			twoChild = child;
			twoChildON = true;
		} else
			Utils.error("Adding 3rd Child is not allowed");

		if (sameChildInDominotorAndCFG)
			child.updateAnchorLastAccessAndPhi(this);

		child.setParent(this);
	}

	private void nullCheck() {
		if (mBasicBlockList == null)
			mBasicBlockList = new ArrayList<BasicBlock>();
	}

	public void whileFixSingleIns(Instruction i, ArrayList<Instruction> phiInstructions,
			ArrayList<Instruction> phiParams) {
		if (phiParams.contains(i)) {
			int index = phiParams.indexOf(i);
			phiParams.remove(index - 1);
			phiParams.remove(index - 1);
			phiInstructions.remove(index / 2);
			return;
		}

		if (i.getLeftInstruction() != null && phiParams.contains(i.getLeftInstruction()))
			i.setLeftInstruction(phiInstructions.get(phiParams.indexOf(i.getLeftInstruction()) / 2));

		if (i.getRightInstruction() != null && phiParams.contains(i.getRightInstruction()))
			i.setRightInstruction(phiInstructions.get(phiParams.indexOf(i.getRightInstruction()) / 2));
	}

	public void whileFix(ArrayList<Instruction> phiInstructions, ArrayList<Instruction> phiParams) {
		if(fixList.contains(this))
			return;

		fixList.add(this);

		for (Instruction i : mInstructionSet) {
			whileFixSingleIns(i, phiInstructions, phiParams);
			if (i.getCode() != CODE.phi)
				continue;

			String Str = i.getPhiFor();
			for (int j = phiInstructions.size() - 1; j >= 0; j--) {
				Instruction phi = phiInstructions.get(j);
				if (phi.getPhiFor().equals(Str)) {
					phiInstructions.remove(phi);
					phiParams.remove(2 * j);
					phiParams.remove(2 * j);
				}
			}
		}

		if (phiInstructions.isEmpty())
			return;

		if (oneChildON)
			oneChild.whileFix(phiInstructions, phiParams);

		if (twoChildON)
			twoChild.whileFix(phiInstructions, phiParams);
	}

	public void fixPhis() {
		ArrayList<Instruction> phiInstructions = null, phiParams = null;

		phiInstructions = new ArrayList<Instruction>();
		phiParams = new ArrayList<Instruction>();

		for (Instruction i : mInstructionSet) {
			if (i.getCode() != CODE.phi) {
				whileFixSingleIns(i, phiInstructions, phiParams);
				continue;
			}

			phiInstructions.add(i);
			phiParams.add(i.getLeftInstruction());
			phiParams.add(i.getRightInstruction());
		}
		fixList = new ArrayList<BasicBlock>();
		fixList.add(this);
		oneChild.whileFix(phiInstructions, phiParams);
		fixList.clear();
		phiInstructions.clear();
		phiParams.clear();
		phiInstructions = null;
		phiParams = null;
		fixList = null;
	}

	public void fixUp() {
		if (mInstructionSet.isEmpty() && (oneChildON || twoChildON))
			mInstructionSet.add(Instruction.getInstruction(CODE.BSR, false));
		else if (mInstructionSet.isEmpty())
			return;

		Instruction lastInstruction = getLastInstruction();
		if (!toBeFixed.contains(lastInstruction.getCode()))
			return;

		if (lastInstruction.getCode() == CODE.BSR) {
			lastInstruction.fixup(oneChild.getFirstInstruction());
		} else
			lastInstruction.fixup(twoChild.getFirstInstruction());
	}

	public boolean hasInstructions() {
		return true;
/*
		if (mInstructionSet.isEmpty())
			return false;

		if (mInstructionSet.size() == 1)
			return !(mInstructionSet.get(0).getCode() == CODE.BSR);
		return true;*/
	}

	public boolean secondParentExists() {
		return twoParentON;
	}

	public boolean firstParentExists() {
		return oneParentON;
	}

	public boolean secondChildExists() {
		return twoChildON;
	}

	public boolean firstChildExists() {
		return oneChildON;
	}

	public String getFunctionName() {
		return mFunctionName;
	}

	public int getIndex() {
		return index;
	}

	public BasicBlock getSecondChild() {
		return twoChild;
	}

	public BasicBlock getFirstChild() {
		return oneChild;
	}

	public BasicBlock getSecondParent() {
		return twoParent;
	}

	public BasicBlock getFirstParent() {
		return oneParent;
	}

	public static ArrayList<BasicBlock> getBasicBlockList() {
		return mBasicBlockList;
	}

	public Instruction getLastInstruction() {
		return mInstructionSet.get(mInstructionSet.size() - 1);
	}

	public void updateLastAccessFor(String key, Instruction value) {
		lastAccessTable.put(key, value);
	}

	public Instruction getLastAccessFor(String value) {
		if (!lastAccessTable.containsKey(value))
			return null;
		return lastAccessTable.get(value);
	}

	public Instruction getFirstInstruction() {
		if (mInstructionSet.isEmpty() && (oneChildON || twoChildON))
			mInstructionSet.add(Instruction.getInstruction(CODE.BSR, false));

		if (!mInstructionSet.isEmpty())
			return mInstructionSet.get(0);
		return null;
	}

	public boolean shouldIgnore() {
		return ignore;
	}

	public void ignore() {
		ignore = true;
	}

	@Override
	public String toString() {
		String returnString = "";
		if (!mInstructionSet.isEmpty()) {
			returnString = "\nTAG: " + TAG + "(" + index + ") "+" func = "+mFunctionName;
			for (Instruction i : mInstructionSet)
				returnString += "\n" + i;
			return returnString;
		}
		return "\nTAG: " + TAG + "(" + index + ")  THIS IS LAST BB, we have no Instructions because we are a sucks !!";
	}
}