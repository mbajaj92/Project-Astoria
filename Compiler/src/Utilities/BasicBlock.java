package Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import Utilities.Utils.CODE;

public class BasicBlock {
	private HashSet<Integer> liveRange = null;
	private static ArrayList<BasicBlock> fixList;
	private String TAG, mFunctionName;
	private boolean ignore = false;
	private static ArrayList<BasicBlock> mBasicBlockList;
	private int index = -1;
	private boolean visited = false, isLast = false, oneChildON = false, twoChildON = false, oneParentON = false, twoParentON = false;
	private BasicBlock oneChild, twoChild, oneParent, twoParent;
	private ArrayList<Instruction> mInstructionSet = null;
	private List<CODE> toBeFixed = Arrays.asList(CODE.BGE, CODE.BEQ, CODE.BGT, CODE.BLE, CODE.BLT, CODE.BNE, CODE.BRA);
	private static List<CODE> notToBeAnchored = Arrays.asList(CODE.phi, CODE.BEQ, CODE.BGE, CODE.BLE, CODE.BGT,
			CODE.BLT, CODE.BNE);
	private HashMap<CODE, Instruction> anchor = null;
	private HashMap<String, Instruction> lastAccessTable = null;

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

	public void addInstruction(Instruction i, boolean toAnchor) {
		CODE code = i.getCode();
		if (toAnchor && !notToBeAnchored.contains(code)) {

			if (anchor.containsKey(code))
				i.setPreviousInAnchor(anchor.get(code));

			if (code == CODE.store)
				anchor.remove(CODE.adda);

			anchor.put(code, i);
		}

		if (code == CODE.phi)
			mInstructionSet.add(0, i.setBasicBlock(this));
		else
			mInstructionSet.add(i.setBasicBlock(this));
	}

	public void setAsCurrent() {
		mBasicBlockList.add(this);
		index = mBasicBlockList.size() - 1;
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
			Instruction right = rightTable.get(key);
			if (!isWhile && left == null)
				Utils.error("Left Chain is null for " + right.toStringConstant(key));
			if (right == null && !isWhile)
				Utils.error("Right Chain is null for " + left.toStringConstant(key));

			if (((isWhile && right != null) || (!isWhile)) && (left != right && !left.isDuplicate(right))) {
				if (isWhile) {
					if (right.getCode() != CODE.load) {
						Instruction i = Instruction.getInstruction(CODE.phi, left, right, false).setPhiFor(key).setBasicBlock(this);
						mInstructionSet.add(0, i);
						updateLastAccessFor(key, i);
					} else
						updateLastAccessFor(key, left);
				} else
					updateLastAccessFor(key, Instruction.getInstruction(CODE.phi, left, right).setPhiFor(key));
			} else if (left != null)
				updateLastAccessFor(key, left);
		}

		for (String key : rightTable.keySet()) {
			Instruction left = leftTable.get(key);
			Instruction right = rightTable.get(key);
			if (left == null && !isWhile)
				Utils.error("Left Chain is null for for " + right.toStringConstant(key));
			if (right == null && !isWhile)
				Utils.error("Right Chain is null for for " + left.toStringConstant(key));

			if ((isWhile && left == null)
					|| ((((isWhile && right != null) || (!isWhile)) && (left != right && !right.isDuplicate(left))))
							&& !lastAccessTable.containsKey(key)) {
				if (isWhile) {
					if (right.getCode() != CODE.load) {
						Instruction i = Instruction.getInstruction(CODE.phi, left, right, false).setPhiFor(key).setBasicBlock(this);
						mInstructionSet.add(0, i);
						updateLastAccessFor(key, i);
					} else
						updateLastAccessFor(key, right);
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

	public void generetePhiAndUpdateTree(HashMap<String, Instruction> leftTable,
			HashMap<String, Instruction> rightTable, HashMap<CODE, Instruction> parentA) throws Exception {
		takeCareOfPhi(leftTable, rightTable, true);
		fixPhis(new HashMap<CODE, Instruction>(parentA), new HashMap<String, Instruction>(leftTable));
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

	private void replaceIfRequired(Instruction i, HashMap<Instruction, Instruction> replaceList) {
		if (i.getLeftInstruction() != null && replaceList.containsKey(i.getLeftInstruction()))
			i.setLeftInstruction(replaceList.get(i.getLeftInstruction()));

		if (i.getRightInstruction() != null && replaceList.containsKey(i.getRightInstruction()))
			i.setRightInstruction(replaceList.get(i.getRightInstruction()));
	}

	private void whileFixSingleIns(Instruction i, ArrayList<Instruction> phiInstructions,
			ArrayList<Instruction> phiParams, HashMap<Instruction, Instruction> replaceList,
			HashMap<CODE, Instruction> parentAnchor, HashMap<String, Instruction> parentLastAccess) {

		if (!i.isLoadForArray() && i.getCode() == CODE.load) {
			boolean broken = false;
			//Utils.SOPln("index = " + i.getIndex()+" right =  "+i.getRightConstant());
			for (Instruction ins : phiInstructions) {
				if (i.getRightConstant().equals(ins.getPhiFor())) {
					replaceList.put(i, ins);
					broken = true;
					break;
				}
			}

			if (!broken) {
				if (parentLastAccess == null)
					Utils.SOPln("PARENT LAST ACCESS IS NULL BCBC ");
				else {
					Instruction last = parentLastAccess.get(i.getRightConstant());
					if (last != null)
						replaceList.put(i, last);
				}
			}

		} else {
			replaceIfRequired(i, replaceList);

			if (i.getLeftInstruction() != null && phiParams.contains(i.getLeftInstruction())) {
				Instruction phi = phiInstructions.get(phiParams.indexOf(i.getLeftInstruction()) / 2);
				if (phi.getPhiFor().equals(i.getAInsFor())) {
					i.setLeftInstruction(phi);
					i.clearReferenceInstruction();
				}
			}

			if (i.getRightInstruction() != null && phiParams.contains(i.getRightInstruction())) {
				Instruction phi = phiInstructions.get(phiParams.indexOf(i.getRightInstruction()) / 2);
				if (phi.getPhiFor().equals(i.getBInsFor())) {
					i.setRightInstruction(phi);
					i.clearReferenceInstruction();
				}
			}

			i.anchorTest(parentAnchor);
			if (i.hasReferenceInstruction() && !(i == i.getReferenceInstruction())) {
				replaceList.put(i, i.getReferenceInstruction());
			} else if (i.hasReferenceInstruction())
				i.clearReferenceInstruction();
		}

		if (i.getCode() != CODE.phi && phiParams.contains(i)) {
			int index = phiParams.indexOf(i);
			phiParams.remove(index - 1);
			phiParams.remove(index - 1);
			phiInstructions.remove(index / 2);
		}
	}

	public void whileFix(ArrayList<Instruction> phiInstructions, ArrayList<Instruction> phiParams,
			HashMap<Instruction, Instruction> replaceList, HashMap<CODE, Instruction> parentAnchor,
			HashMap<String, Instruction> parentLastAccess) {
		if (fixList.contains(this))
			return;

		fixList.add(this);

		for (int j = 0; j < mInstructionSet.size(); j++) {
			Instruction i = mInstructionSet.get(j);
			whileFixSingleIns(i, phiInstructions, phiParams, replaceList, parentAnchor, parentLastAccess);
			if (replaceList.containsKey(i)) {
				mInstructionSet.remove(i);
				j--;
			}

			if (i.getCode() != CODE.phi)
				continue;

			String Str = i.getPhiFor();
			for (int k = phiInstructions.size() - 1; k >= 0; k--) {
				Instruction phi = phiInstructions.get(k);
				if (phi.getPhiFor().equals(Str)) {
					if(i.getLeftInstruction() == null)
						i.setLeftInstruction(phi);

					phiInstructions.remove(phi);
					phiParams.remove(2 * k);
					phiParams.remove(2 * k);
				}
			}
		}

		if (oneChildON)
			oneChild.whileFix(phiInstructions, phiParams, replaceList, parentAnchor, parentLastAccess);

		if (twoChildON)
			twoChild.whileFix(phiInstructions, phiParams, replaceList, parentAnchor, parentLastAccess);
	}

	public void fixPhis(HashMap<CODE, Instruction> parentAnchor, HashMap<String, Instruction> parentLastAccess) {
		ArrayList<Instruction> phiInstructions = null, phiParams = null;
		HashMap<Instruction, Instruction> replaceList = new HashMap<Instruction, Instruction>();

		phiInstructions = new ArrayList<Instruction>();
		phiParams = new ArrayList<Instruction>();

		for (int j = 0; j < mInstructionSet.size(); j++) {
			Instruction i = mInstructionSet.get(j);
			if (i.getCode() != CODE.phi) {
				whileFixSingleIns(i, phiInstructions, phiParams, replaceList, parentAnchor, parentLastAccess);
				if (replaceList.containsKey(i)) {
					mInstructionSet.remove(i);
					j--;
				}
				continue;
			}

			phiInstructions.add(i);
			phiParams.add(i.getLeftInstruction());
			phiParams.add(i.getRightInstruction());
		}

		fixList = new ArrayList<BasicBlock>();
		fixList.add(this);
		oneChild.whileFix(phiInstructions, phiParams, replaceList, parentAnchor, parentLastAccess);
		fixList.clear();
		phiInstructions.clear();
		phiParams.clear();
		phiInstructions = null;
		phiParams = null;
		fixList = null;
	}

	public void fixUp() {
		if (mInstructionSet.isEmpty() && (oneChildON || twoChildON))
			mInstructionSet.add(Instruction.getInstruction(CODE.BRA, false).setBasicBlock(this));
		else if (mInstructionSet.isEmpty())
			return;

		Instruction lastInstruction = getLastInstruction();
		if (!toBeFixed.contains(lastInstruction.getCode()))
			return;

		if (lastInstruction.getCode() == CODE.BRA)
			lastInstruction.setLeftInstruction(oneChild.getFirstInstruction());
		else
			lastInstruction.setRightInstruction(twoChild.getFirstInstruction());
	}

	public boolean hasInstructions() {
		return true;
		/*
		 * if (mInstructionSet.isEmpty()) return false;
		 * 
		 * if (mInstructionSet.size() == 1) return
		 * !(mInstructionSet.get(0).getCode() == CODE.BSR); return true;
		 */
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

	public HashMap<CODE, Instruction> getAnchor() {
		return anchor;
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

	
	public HashSet<Integer> getLiveRange() {
		if (liveRange == null)
			liveRange = new HashSet<Integer>();
		return liveRange;
	}

	public void addLiveRange(HashSet<Integer> live) {
		if (liveRange == null)
			liveRange = new HashSet<Integer>();
		liveRange.addAll(live);
	}
	
	public void visitAndUpdateLiveRange(HashSet<Integer> live) {
		addLiveRange(live);

		for (int i = mInstructionSet.size() - 1; i >= 0; i--) {
			Instruction current = mInstructionSet.get(i);
			if (current.getCode() == CODE.phi || current.getCode() == CODE.BRA)
				continue;

			int currentIndex = current.getInstructionNumber();
			if (getTagtype() != "LOOP_HEADER" || isVisited())
				liveRange.remove((Integer) currentIndex);

			for (int j : liveRange)
				if (j != currentIndex)
					Utils.addEdge(currentIndex, j);

			if (current.getLeftInstruction() != null)
				liveRange.add(current.getLeftInstruction().getInstructionNumber());

			if (!Utils.compareInstructions.contains(current.getCode()) && current.getRightInstruction() != null)
				liveRange.add(current.getRightInstruction().getInstructionNumber());
		}
		visited = true;
	}

	public boolean isVisited() {
		return visited;
	}

	public void setLast() {
		isLast = true;
	}

	public boolean isLastBlock() {
		return isLast;
	}

	public ArrayList<Instruction> getInstructionList() {
		return mInstructionSet;
	}

	public String getTagtype() {
		if (TAG.contains("LOOP_HEADER"))
			return "LOOP_HEADER";
		else if (TAG.contains("IF_HEADER"))
			return "IF_HEADER";
		return "REGULAR";
	}

	public boolean areBothChildrenVisited() {
		return isFirstChildVisited() && isSecondChildVisited();
	}

	public boolean isFirstChildVisited() {
		boolean flag = false;
		if (!firstChildExists())
			return true;
		else {
			if (getFirstChild().isVisited())
				return true;
		}
		return flag;
	}

	public boolean isSecondChildVisited() {
		boolean flag = false;
		if (!secondChildExists())
			return true;
		else {
			if (getSecondChild().isVisited())
				return true;
		}
		return flag;
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
			mInstructionSet.add(Instruction.getInstruction(CODE.BRA, false).setBasicBlock(this));

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
			returnString = "\nTAG: " + TAG + "(" + index + ") " + " func = " + mFunctionName;
			for (Instruction i : mInstructionSet)
				returnString += "\n" + i;
			return returnString;
		}
		return "\nTAG: " + TAG + "(" + index + ")  THIS IS LAST BB, we have no Instructions because we are a sucks !!";
	}
}