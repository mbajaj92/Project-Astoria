package Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import Utilities.Utils.CODE;

public class BasicBlock {
	public String TAG;
	private boolean ignore = false;
	public static ArrayList<BasicBlock> basicBlockList;
	public int index = -1;
	public boolean oneON = false, twoON = false;
	public BasicBlock oneChild, twoChild;
	private ArrayList<Instruction> mInstructionSet = null;
	private List<CODE> toBeFixed = Arrays.asList(CODE.BGE,CODE.BEQ,CODE.BGT,CODE.BLE,CODE.BLT,CODE.BNE, CODE.BSR);
	private static List<CODE> notToBeAnchored = Arrays.asList(CODE.phi, CODE.BEQ, CODE.BGE, CODE.BLE, CODE.BGT,
			CODE.BLT, CODE.BNE);
	private HashMap<CODE,Instruction> anchor = null;
	private HashMap<String,Instruction> lastAccessTable = null;
	
	BasicBlock(String tag) {
		TAG = tag;
		nullCheck();
		mInstructionSet = new ArrayList<Instruction>();
		anchor = new HashMap<CODE,Instruction>();
		lastAccessTable = new HashMap<String, Instruction>();
		basicBlockList.add(this);
		index = basicBlockList.size() - 1;

	}

	public Instruction getAnchorInstructionForCode(CODE code) {
		if (anchor.containsKey(code))
			return anchor.get(code);
		return null;
	}

	public void addInstruction(Instruction i, boolean updateAnchor) {
		CODE code = i.getCode();
		if (updateAnchor  && !notToBeAnchored.contains(code)) {
			//TODO: take care of LDW and STX

			if (anchor.containsKey(code))
				i.setPreviousInAnchor(anchor.get(code));

			anchor.put(code, i);
		}
		mInstructionSet.add(i);
	}

	public void setAsCurrent() {
		basicBlockList.add(this);
		index = basicBlockList.size()-1;
	}

	public HashMap<String, Instruction> getLastAccessTable() {
		return lastAccessTable;
	}

	public static BasicBlock getCurrentBasicBlock() {
		return basicBlockList.get(basicBlockList.size() - 1);
	}

	public void copyParentLastAccess(BasicBlock parent) {
		lastAccessTable.putAll(parent.lastAccessTable);
	}

	private void takeCareOfPhi(HashMap<String, Instruction> leftTable, HashMap<String, Instruction> rightTable) {
		for (String key : leftTable.keySet()) {
			Instruction left = leftTable.get(key);
			Instruction right = rightTable.get(key);
			if (left != right)
				updateLastAccessFor(key, Instruction.getInstruction(CODE.phi, left, right).setPhiFor(key));
			else
				updateLastAccessFor(key, left);
		}

		for (String key : rightTable.keySet()) {
			Instruction left = leftTable.get(key);
			Instruction right = rightTable.get(key);
			if (left != right && !lastAccessTable.containsKey(key))
				updateLastAccessFor(key, Instruction.getInstruction(CODE.phi, left, right).setPhiFor(key));
			else if (!lastAccessTable.containsKey(key))
				updateLastAccessFor(key, right);
		}
	}

	private void updateLastAccess(BasicBlock parent) {
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

	public void updateAnchorAndPhi(BasicBlock parent) {
		updateAnchorAndPhi(parent, null, null);
	}

	public void updateAnchorAndPhi(BasicBlock parent, HashMap<String, Instruction> leftParent,
			HashMap<String, Instruction> rightParent) {
		anchor.putAll(parent.anchor);

		if (leftParent != null && rightParent != null)
			takeCareOfPhi(leftParent, rightParent);
		updateLastAccess(parent);
	}

	public void deleteLastInstruction() {
		mInstructionSet.remove(mInstructionSet.size() - 1);
	}

	public void replaceChild(BasicBlock prevChild, BasicBlock newChild) throws Exception {
		Utils.SOPln("Here to replace "+prevChild.index+"  TAG: "+prevChild.TAG);
		if (oneChild == prevChild) {
			Utils.SOPln("ONE HIT");
			oneChild.ignore();
			oneChild = newChild;
		} else if (twoChild == prevChild) {
			Utils.SOPln("TWO HIT");
			twoChild.ignore();
			twoChild = newChild;
		} else
			Utils.error("NOT MY CHILD");
		newChild.updateAnchorAndPhi(this);
		newChild.copyParentLastAccess(this);
	}

	public void replace1stChild(BasicBlock child) throws Exception {
		oneChild.ignore();
		oneON = false;
		setChild(child, false);
	}

	public void replace2ndChild(BasicBlock child) throws Exception {
		twoChild.ignore();
		twoON = false;
		setChild(child, false);
	}

	public void setChild(BasicBlock child, boolean sameChildInDominotorAndCFG) throws Exception {
		if (!oneON) {
			oneChild = child;
			oneON = true;
		} else if (!twoON) {
			twoChild = child;
			twoON = true;
		} else
			Utils.error("Adding 3rd Child is not allowed");

		if(sameChildInDominotorAndCFG) {
			child.updateAnchorAndPhi(this);
			child.copyParentLastAccess(this);
		}
	}

	public void addPhi(String value, Instruction ins, boolean left) {
		nullCheck();
		Instruction phiInstruction = null;
		Instruction leftIns = null, rightIns = null;
		for (Instruction i : mInstructionSet) {
			if (i.getCode() == CODE.phi && i.getPhiFor() != null ? i.getPhiFor().equals(value) : false) {
				phiInstruction = i;
				break;
			}
		}

		if (left) {
			if (phiInstruction != null)
				phiInstruction.aInstruction = ins;
			else
				leftIns = ins;
		} else {
			if (phiInstruction != null)
				phiInstruction.bInstruction = ins;
			else
				rightIns = ins;
		}

		if (phiInstruction == null)
			mInstructionSet.add(0,Instruction.getInstruction(CODE.phi, leftIns, rightIns).setPhiFor(value));
	}

	private void nullCheck() {
		if (basicBlockList == null)
			basicBlockList = new ArrayList<BasicBlock>();
	}

	public void whileFixSingleIns(Instruction i, ArrayList<Integer> phiInstructions, ArrayList<Integer> phiParams) {
		if (i.aInstruction != null && phiParams.contains(i.aInstruction.index)) {
			i.aInstruction = Instruction.instructionList
					.get(phiInstructions.get(phiParams.indexOf(i.aInstruction.index) / 2));
		}

		if (i.bInstruction != null && phiParams.contains(i.bInstruction.index))
			i.bInstruction = Instruction.instructionList
					.get(phiInstructions.get(phiParams.indexOf(i.bInstruction.index) / 2));
	}

	public void whileFix(BasicBlock stopBock, ArrayList<Integer> phiInstructions, ArrayList<Integer> phiParams) {
		if(this == stopBock)
			return;

		for (Instruction i : mInstructionSet)
			whileFixSingleIns(i,phiInstructions,phiParams);

		if (oneON)
			oneChild.whileFix(stopBock, phiInstructions, phiParams);

		if (twoON)
			twoChild.whileFix(stopBock, phiInstructions, phiParams);
	}

	public void fixPhis(BasicBlock leftBlock, BasicBlock rightBlock, boolean isWhile) {
		ArrayList<Integer> phiInstructions = null, phiParams = null;
		if (isWhile) {
			phiInstructions = new ArrayList<Integer>();
			phiParams = new ArrayList<Integer>();
		}

		for (Instruction i : mInstructionSet) {
			if ((i.getCode() != CODE.phi) || (i.bInstruction != null && i.aInstruction != null)) {
				if(isWhile)
					whileFixSingleIns(i, phiInstructions, phiParams);
				continue;
			}

			if (isWhile)
				phiInstructions.add(i.index);

			if (i.aInstruction == null) {
				if (!leftBlock.lastAccessTable.containsKey(i.getPhiFor())) {
					i.warning = Instruction.toStringConstant(i.getPhiFor()) + " doesn't exist in left chain";
				} else
					i.aInstruction = leftBlock.lastAccessTable.get(i.getPhiFor());
			} else {
				if (!rightBlock.lastAccessTable.containsKey(i.getPhiFor())) {
					i.warning = Instruction.toStringConstant(i.getPhiFor()) + " doesn't exist in right chain";
				} else
					i.bInstruction = rightBlock.lastAccessTable.get(i.getPhiFor());
			}

			if (isWhile) {
				phiParams.add(i.aInstruction.index);
				phiParams.add(i.bInstruction.index);
			}
		}

		if (isWhile) {
			Utils.SOPln(phiInstructions);
			Utils.SOPln(phiParams);
			oneChild.whileFix(this, phiInstructions, phiParams);
			phiInstructions.clear();
			phiParams.clear();
			phiInstructions = null;
			phiParams = null;
		}
	}

	public void fixUp() {
		Utils.SOPln("\nFIXING UP BLOCK "+TAG+" index = "+index+"\n-------\n");
		if (mInstructionSet.isEmpty())
			mInstructionSet.add(Instruction.getInstruction(CODE.BSR,false));

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
		if (mInstructionSet.isEmpty())
			mInstructionSet.add(Instruction.getInstruction(CODE.BSR,false));

		int counter = 0;
		while (counter < mInstructionSet.size() && mInstructionSet.get(counter).hasReferenceInstruction())
			counter++;
		if (counter < mInstructionSet.size())
			return mInstructionSet.get(counter);

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
			returnString = "\nTAG: " + TAG + "(" + index + ")";
			for (Instruction i : mInstructionSet)
				returnString += "\n" + i;
			return returnString;
		}
		return "\nTAG: " + TAG + "(" + index + ")  THIS IS LAST BB, we have no Instructions because we are a sucks !!";
	}
}