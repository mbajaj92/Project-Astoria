package Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import Utilities.Utils.BasicBlockType;
import Utilities.Utils.CODE;

public class BasicBlock {
	public int myWhileDepth = 0;
	private HashSet<Integer> liveRange = null, leftOver = null;
	private static ArrayList<BasicBlock> fixList;
	private String TAG, mFunctionName;
	private static ArrayList<BasicBlock> mBasicBlockList;
	private int index = -1;
	private boolean visited = false, bvisited = false, isLast = false, oneChildON = false, twoChildON = false, oneParentON = false,
			twoParentON = false, ignore = false;
	private BasicBlockType type;
	private BasicBlock oneChild, twoChild, oneParent, twoParent;
	private ArrayList<Instruction> mInstructionSet = null;
	private List<CODE> toBeFixed = Arrays.asList(CODE.BGE, CODE.BEQ, CODE.BGT, CODE.BLE, CODE.BLT, CODE.BNE, CODE.BSR);
	private static List<CODE> notToBeAnchored = Arrays.asList(CODE.CMP, CODE.CMPI, CODE.phi, CODE.BEQ, CODE.BGE,
			CODE.BLE, CODE.BGT, CODE.BLT, CODE.BNE, CODE.read, CODE.write, CODE.writeNL, CODE.RET);
	private HashMap<CODE, Instruction> anchor = null;
	private HashMap<String, Instruction> lastAccessTable = null;

	BasicBlock(String tag) {
		this(tag, null);
	}

	BasicBlock(String tag, String name) {
		TAG = tag;
		setType(TAG);
		mFunctionName = name;
		nullCheck();
		mInstructionSet = new ArrayList<Instruction>();
		anchor = new HashMap<CODE, Instruction>();
		lastAccessTable = new HashMap<String, Instruction>();
		mBasicBlockList.add(this);
		index = mBasicBlockList.size() - 1;
		myWhileDepth = Utils.WHILE_DEPTH;
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
		lastAccessTable = new HashMap<String, Instruction>(parent.lastAccessTable);
	}

	private void takeCareOfPhi(HashMap<String, Instruction> leftTable, HashMap<String, Instruction> rightTable,
			boolean isWhile) throws Exception {
		for (String key : leftTable.keySet()) {
			Instruction left = leftTable.get(key);
			Instruction right = rightTable.get(key);
			/*if (!isWhile && left == null)
				Utils.error("Left Chain is null for " + right.toStringConstant(key));
			if (right == null && !isWhile)
				Utils.error("Right Chain is null for " + left.toStringConstant(key));*/

			if (((isWhile && right != null) || (!isWhile)) && (left != right && !left.isDuplicate(right))) {
				if (isWhile) {
					if (right.getCode() != CODE.load) {
						Instruction i = Instruction.getInstruction(CODE.phi, left, right, false).setPhiFor(key)
								.setBasicBlock(this);
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
			/*if (left == null && !isWhile)
				Utils.error("Left Chain is null for for " + right.toStringConstant(key));
			if (right == null && !isWhile)
				Utils.error("Right Chain is null for for " + left.toStringConstant(key));*/

			if ((isWhile && left == null)
					|| ((((isWhile && right != null) || (!isWhile)) && (left != right && !right.isDuplicate(left))))
							&& !lastAccessTable.containsKey(key)) {
				if (isWhile) {
					if (right.getCode() != CODE.load) {
						Instruction i = Instruction.getInstruction(CODE.phi, left, right, false).setPhiFor(key)
								.setBasicBlock(this);
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
			lastAccessTable = new HashMap<String, Instruction>(parent.lastAccessTable);
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
		anchor = new HashMap<CODE, Instruction>(parent.anchor);

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
	
	public void setTag(String tag) {
		TAG = tag;
		setType(TAG);
	}

	public void setChild(BasicBlock child, boolean sameChildInDominotorAndCFG) throws Exception {
		setChild(child, sameChildInDominotorAndCFG, false);
	}

	public void setChild(BasicBlock child, boolean sameChildInDominotorAndCFG, boolean firstReplace) throws Exception {
		if (!oneChildON || firstReplace) {
			oneChild = child;
			oneChildON = true;
		} else if (!twoChildON) {
			twoChild = child;
			twoChildON = true;
		} else
			Utils.error("Adding 3rd Child is not allowed");

		if (sameChildInDominotorAndCFG)
			child.updateAnchorLastAccessAndPhi(this);

		if (ignore)
			child.setIgnore();

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
			ArrayList<Instruction> leftPhiParams, ArrayList<Instruction> rightPhiParams,
			/*ArrayList<Instruction> phiParams,*/ HashMap<Instruction, Instruction> replaceList,
			HashMap<CODE, Instruction> parentAnchor, HashMap<String, Instruction> parentLastAccess) {

		if (!i.isLoadForArray() && i.getCode() == CODE.load) {
			boolean broken = false;
			// Utils.SOPln("index = " + i.getIndex()+" right =
			// "+i.getRightConstant());
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

			if (i.getLeftInstruction() != null && leftPhiParams.contains(i.getLeftInstruction())) {
				Instruction phi = phiInstructions.get(leftPhiParams.indexOf(i.getLeftInstruction()));
				if (phi.getPhiFor().equals(i.getAInsFor())) {
					i.setLeftInstruction(phi);
					i.clearReferenceInstruction();
				}
			}

			if (i.getLeftInstruction() != null && rightPhiParams.contains(i.getLeftInstruction())) {
				Instruction phi = phiInstructions.get(rightPhiParams.indexOf(i.getLeftInstruction()));
				if (phi.getPhiFor().equals(i.getAInsFor())) {
					i.setLeftInstruction(phi);
					i.clearReferenceInstruction();
				}
			}

			if (i.getRightInstruction() != null && leftPhiParams.contains(i.getRightInstruction())) {
				Instruction phi = phiInstructions.get(leftPhiParams.indexOf(i.getRightInstruction()));
				if (phi.getPhiFor().equals(i.getBInsFor())) {
					i.setRightInstruction(phi);
					i.clearReferenceInstruction();
				}
			}

			if (i.getRightInstruction() != null && rightPhiParams.contains(i.getRightInstruction())) {
				Instruction phi = phiInstructions.get(rightPhiParams.indexOf(i.getRightInstruction()));
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

		if (i.getCode() != CODE.phi && leftPhiParams.contains(i)) {
			int index = leftPhiParams.indexOf(i);
			leftPhiParams.remove(index);
			leftPhiParams.add(index,null);
		}
		
		if (i.getCode() != CODE.phi && rightPhiParams.contains(i)) {
			int index = rightPhiParams.indexOf(i);
			rightPhiParams.remove(index);
			rightPhiParams.add(index,null);
		}
	}

	public void whileFix(ArrayList<Instruction> phiInstructions, ArrayList<Instruction> leftPhiParams,
			ArrayList<Instruction> rightPhiParams, HashMap<Instruction, Instruction> replaceList,
			HashMap<CODE, Instruction> parentAnchor, HashMap<String, Instruction> parentLastAccess) {
		if (fixList.contains(this))
			return;

		fixList.add(this);

		for (int j = 0; j < mInstructionSet.size(); j++) {
			Instruction i = mInstructionSet.get(j);
			whileFixSingleIns(i, phiInstructions, leftPhiParams, rightPhiParams, replaceList, parentAnchor,
					parentLastAccess);
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
					if (i.getLeftInstruction() == null)
						i.setLeftInstruction(phi);

					if (i.getRightInstruction() == null)
						i.setRightInstruction(phi);

					phiInstructions.remove(phi);
					leftPhiParams.remove(k);
					rightPhiParams.remove(k);
				}
			}
		}

		if (oneChildON)
			oneChild.whileFix(new ArrayList<Instruction>(phiInstructions), new ArrayList<Instruction>(leftPhiParams),
					new ArrayList<Instruction>(rightPhiParams), new HashMap<Instruction, Instruction>(replaceList),
					parentAnchor, parentLastAccess);

		if (twoChildON)
			twoChild.whileFix(new ArrayList<Instruction>(phiInstructions), new ArrayList<Instruction>(leftPhiParams),
					new ArrayList<Instruction>(rightPhiParams), new HashMap<Instruction, Instruction>(replaceList),
					parentAnchor, parentLastAccess);
	}

	public void fixPhis(HashMap<CODE, Instruction> parentAnchor, HashMap<String, Instruction> parentLastAccess) {
		ArrayList<Instruction> phiInstructions = null, /*phiParams = null,*/ leftPhiParams = null, rightPhiParams = null;
		HashMap<Instruction, Instruction> replaceList = new HashMap<Instruction, Instruction>();

		phiInstructions = new ArrayList<Instruction>();
		leftPhiParams = new ArrayList<Instruction>();
		rightPhiParams = new ArrayList<Instruction>();

		for (int j = 0; j < mInstructionSet.size(); j++) {
			Instruction i = mInstructionSet.get(j);
			if (i.getCode() != CODE.phi) {
				whileFixSingleIns(i, phiInstructions, leftPhiParams, rightPhiParams, replaceList, parentAnchor, parentLastAccess);
				if (replaceList.containsKey(i)) {
					mInstructionSet.remove(i);
					j--;
				}
				continue;
			}

			phiInstructions.add(i);
			leftPhiParams.add(i.getLeftInstruction());
			rightPhiParams.add(i.getRightInstruction());
			/*phiParams.add(i.getLeftInstruction());
			phiParams.add(i.getRightInstruction());*/
		}

		fixList = new ArrayList<BasicBlock>();
		fixList.add(this);
		oneChild.whileFix(phiInstructions, leftPhiParams, rightPhiParams, replaceList, parentAnchor, parentLastAccess);
		fixList.clear();
		phiInstructions.clear();
		leftPhiParams.clear();
		leftPhiParams = null;
		rightPhiParams.clear();
		rightPhiParams = null;
		phiInstructions = null;
		fixList = null;
	}

	public void fixLeftOverTrace(ArrayList<BasicBlock> stop, int i) {
		//Utils.SOPln("CALLING to FIX LEFT OVER FOR " + i + " in BB = " + getIndex());
		if (stop.contains(this))
			return;

		//Utils.SOPln("FIXING LEFT OVER FOR " + i + " in BB = " + getIndex());
		stop.add(this);
		Utils.createTraceEdges(i, mInstructionSet);

		if (firstChildExists())
			getFirstChild().fixLeftOverTrace(stop, i);

		if (secondChildExists())
			getSecondChild().fixLeftOverTrace(stop, i);
	}

	public void fixUp() {
		if (mInstructionSet.isEmpty() && (oneChildON || twoChildON))
			mInstructionSet.add(Instruction.getInstruction(CODE.BSR, false).setBasicBlock(this));

		else if (mInstructionSet.isEmpty())
			return;

		Instruction lastInstruction = getLastInstruction();
		if (!toBeFixed.contains(lastInstruction.getCode()))
			return;

		if (lastInstruction.getCode() == CODE.BSR)
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

	public HashSet<Integer> getLeftOver() {
		if (leftOver == null)
			leftOver = new HashSet<Integer>();
		return leftOver;
	}

	public void addLeftOver(HashSet<Integer> left) {
		if (leftOver == null)
			leftOver = new HashSet<Integer>();
		leftOver.addAll(left);
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

	public boolean isDeadInstruction(Instruction i) throws Exception {
		CODE code = i.getCode();
		if (Utils.isNotDeadCode.contains(code))
			return false;

		if(i.global())
			return false;	

		if (!liveRange.contains((Integer) i.getInstructionNumber()))
			return true;
		
		return false;
	}

	private void mergeAnchor(HashMap<CODE, Instruction> parentAnchor) {
		nullCheck();
		for(CODE key: parentAnchor.keySet()) {
			if(!anchor.containsKey(key))
				anchor.put(key, parentAnchor.get(key));
			else {
				anchor.get(key).fixAnchorRoot(parentAnchor.get(key));
			}
		}
	}

	private void mergeLastAccess(HashMap<String, Instruction> parentLastAccessTable) {
		nullCheck();
		for (String key : parentLastAccessTable.keySet()) {
			if (!lastAccessTable.containsKey(key))
				lastAccessTable.put(key, parentLastAccessTable.get(key));
		}
	}

	public void merge(HashMap<String, Instruction> parentLastAccessTable, HashMap<CODE, Instruction> parentAnchor) {
		mergeLastAccess(parentLastAccessTable);
		mergeAnchor(parentAnchor);
	}

	public void visitAndUpdateLiveRange(HashSet<Integer> live) throws Exception {
		addLiveRange(live);

		for (int i = mInstructionSet.size() - 1; i >= 0; i--) {
			Instruction current = mInstructionSet.get(i);
			if (current.getCode() == CODE.BSR || current.getCode() == CODE.phi || current.getCode() == CODE.EOF) {
				continue;
			}

			int currentIndex = current.getInstructionNumber();
			/* if (getTagtype() != "LOOP_HEADER" || isVisited()) { */
			if (isDeadInstruction(current)) {
				mInstructionSet.remove(i);
				Utils.leftOverTrace.remove((Integer) currentIndex);
				Utils.SOPln("Removing Instruction " + currentIndex + " from BB " + index);
				continue;
			} else
				liveRange.remove((Integer) currentIndex);


			if (current.getCode() != CODE.write && current.getCode() != CODE.writeNL && current.getCode() != CODE.RET
					&& current.getCode() != CODE.store && !Utils.doNotCreateEdge.contains(current.getCode()))
				Utils.addEdge(currentIndex, liveRange);

			if (current.getLeftInstruction() != null && current.getLeftInstruction().getInstructionNumber() != -1)
				liveRange.add(current.getLeftInstruction().getInstructionNumber());

			if (current.getRightInstruction() != null)
				liveRange.add(current.getRightInstruction().getInstructionNumber());

			if (current.getCode() == CODE.call && current.hasFunctionParameters()) {
				for (Instruction ins : current.getFunctionParams())
					if (ins != null)
						liveRange.add(ins.getInstructionNumber());
			}
		}
		visited = true;
	}

	public boolean isVisited() {
		return visited;
	}

	public void addLastInstruction(Instruction ins) {
		if (mInstructionSet.isEmpty()) {
			mInstructionSet.add(ins);
			return;
		}

		Instruction last = mInstructionSet.get(mInstructionSet.size() - 1);
		if (Utils.compareInstructions.contains(last.getCode()))
			mInstructionSet.add(mInstructionSet.size() - 1, ins);
		else
			mInstructionSet.add(ins);
	}
	
	public void setLast() {
		isLast = true;
	}

	public boolean isIgnored() {
		return ignore;
	}

	public void setIgnore() {
		ignore = true;
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

	public void setType(String tag) {
		type = BasicBlockType.REGULAR;
		if (TAG.contains("LOOP_HEADER"))
			type = BasicBlockType.LOOP_HEADER;
		else if (TAG.contains("IF_HEADER"))
			type = BasicBlockType.IF_HEADER;
		else if (TAG.contains("THEN_BLOCK"))
			type = BasicBlockType.THEN_BLOCK;
		else if (TAG.contains("ELSE_BLOCK"))
			type = BasicBlockType.ELSE_BLOCK;
		else if (TAG.contains("FOLLOW_BLOCK"))
			type = BasicBlockType.FOLLOW_BLOCK;
		else if (TAG.contains("DO_BLOCK"))
			type = BasicBlockType.DO_BLOCK;
		else if (TAG.contains("LOOP_FOLLOW"))
			type = BasicBlockType.LOOP_FOLLOW;
	}

	public boolean areBothParentVisited() {
		return getFirstParent().isbVisited() && getSecondParent().isbVisited();
	}

	private static boolean waitingForFallThrough = false;
	private static HashSet<Integer> waitList = null;

	public void lowerToMachineCode() throws Exception {
		for (Instruction i : mInstructionSet) {
			if (waitingForFallThrough) {
				waitingForFallThrough = false;
				if (Utils.getFixUpMap().containsKey(i.getInstructionNumber()))
					Utils.getFixUpMap().get(i.getInstructionNumber()).addAll(waitList);
				else
					Utils.getFixUpMap().put(i.getInstructionNumber(), waitList);
				waitList = null;
			}

			boolean shouldICheckForFix = true;
			if (i.getCode() == CODE.BSR && Utils.getFixUpMap().containsKey(i.getInstructionNumber())) {
				shouldICheckForFix = false;
				HashSet<Integer> arr = Utils.getFixUpMap().remove(i.getInstructionNumber());
				Instruction jumpTo = i.getLeftInstruction();
				if (jumpTo.myProgramCounter == -1) {
					if (Utils.getFixUpMap().containsKey(jumpTo.getInstructionNumber()))
						Utils.getFixUpMap().get(jumpTo.getInstructionNumber()).addAll(arr);
					else
						Utils.getFixUpMap().put(jumpTo.getInstructionNumber(), arr);
				} else
					for (int index : arr)
						Utils.fixupOld(index, jumpTo.myProgramCounter);

			} else if (i.getCode() == CODE.phi && Utils.getFixUpMap().containsKey(i.getInstructionNumber())) {
				int currentIndex = mInstructionSet.indexOf(i);
				shouldICheckForFix = false;
				HashSet<Integer> arr = Utils.getFixUpMap().remove(i.getInstructionNumber());
				if (currentIndex < mInstructionSet.size() - 1) {
					Instruction jumpTo = mInstructionSet.get(currentIndex + 1);
					if (Utils.getFixUpMap().containsKey(jumpTo.getInstructionNumber()))
						Utils.getFixUpMap().get(jumpTo.getInstructionNumber()).addAll(arr);
					else
						Utils.getFixUpMap().put(jumpTo.getInstructionNumber(), arr);
				} else {
					waitingForFallThrough = true;
					waitList = arr;
				}
			}

			if (shouldICheckForFix && Utils.getFixUpMap().containsKey(i.getInstructionNumber())) {
				HashSet<Integer> arr = Utils.getFixUpMap().remove(i.getInstructionNumber());
				for (int index : arr)
					Utils.fixupOld(index);
			}

			if (i.getCode() == CODE.phi) {
				i.myProgramCounter = Utils.programCounter;
				Utils.SOPln("Assign "+i.getInstructionNumber()+" PC = "+i.myProgramCounter);
				continue;
			}

			if (i.getCode() == CODE.EOF) {
				Utils.put(CODE.RET, 0, 0, 0);
			} else if (Utils.isF1(i.getCode()))
				Utils.handleF1(i);
			else if (Utils.isF2(i.getCode()))
				Utils.handleF2(i);
			else if (i.getCode() == CODE.read)
				Utils.put(CODE.RDD, Utils.getRegisterForColor(i.getColor()), 0, 0);
			else if (i.getCode() == CODE.writeNL)
				Utils.put(CODE.WRL, 0, 0, 0);
			else if (i.getCode() == CODE.writeNL)
				Utils.put(CODE.WRL, 0, 0, 0);
			else if (i.getCode() == CODE.write)
				Utils.put(CODE.WRD, 0, Utils.getRegisterForColor(i.getLeftInstruction().getColor()), 0);
			else if (i.getCode() == CODE.BSR) {
				int pc = Utils.programCounter;
				Utils.put(CODE.BSR, 0, 0, 0);
				if (i.getLeftInstruction().myProgramCounter == -1) {
					if (Utils.getFixUpMap().containsKey(i.getLeftInstruction().getInstructionNumber()))
						Utils.getFixUpMap().get(i.getLeftInstruction().getInstructionNumber()).add(pc);
					else {
						HashSet<Integer> arr = new HashSet<Integer>();
						arr.add(pc);
						Utils.getFixUpMap().put(i.getLeftInstruction().getInstructionNumber(), arr);
					}
				} else
					Utils.fixupOld(pc, i.getLeftInstruction().myProgramCounter);
			} else if (Utils.compareInstructions.contains(i.getCode())) {
				int pc = Utils.programCounter;
				Utils.put(i.getCode(), Utils.getRegisterForColor(i.getLeftInstruction().getColor()), 0, 0);
				if (Utils.getFixUpMap().containsKey(i.getRightInstruction().getInstructionNumber()))
					Utils.getFixUpMap().get(i.getRightInstruction().getInstructionNumber()).add(pc);
				else {
					HashSet<Integer> arr = new HashSet<Integer>();
					arr.add(pc);
					Utils.getFixUpMap().put(i.getRightInstruction().getInstructionNumber(), arr);
				}

			} else if (i.getCode() == CODE.move) {
				Utils.put(CODE.ADD, Utils.getRegisterForColor(i.getRightInstruction().getColor()),
						Utils.getRegisterForColor(Instruction.getZeroInstruction().getColor()),
						Utils.getRegisterForColor(i.getLeftInstruction().getColor()));
			} else {
				Utils.SOPln("Ignoring " + i.getInstructionNumber());
			}

			i.myProgramCounter = Utils.programCounter - 1;
			Utils.SOPln("Assign "+i.getInstructionNumber()+" PC = "+i.myProgramCounter);
			/* Special case instructions need to process further */
		}
		bvisited = true;
	}

	public boolean isbVisited() {
		return bvisited;
	}
	
	public BasicBlockType getBlockType() {
		return type;
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
			mInstructionSet.add(Instruction.getInstruction(CODE.BSR, false).setBasicBlock(this));

		if (!mInstructionSet.isEmpty())
			return mInstructionSet.get(0);
		return null;
	}

	@Override
	public String toString() {
		String returnString = "";
		if (!mInstructionSet.isEmpty()) {
			returnString = "\nTAG: " + TAG + "(" + index + ") " + " func = " + mFunctionName + " WD = " + myWhileDepth
					+ (ignore ? "  IGNORED" : "");
			for (Instruction i : mInstructionSet)
				returnString += "\n" + i;
			return returnString;
		}
		return "\nTAG: " + TAG + "(" + index + ") " + " func = " + mFunctionName + (ignore ? "  IGNORED" : "");
	}

	public static void shutDown() {
		if (fixList != null)
			fixList.clear();
		fixList = null;

		if (mBasicBlockList != null)
			mBasicBlockList.clear();
		mBasicBlockList = null;

	}
}