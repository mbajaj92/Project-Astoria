package Utilities;

import java.util.ArrayList;

import Utilities.Utils.CODE;
import Utilities.Utils.RESULT_KIND;

public class Result {

	public RESULT_KIND kind;
	public boolean isArray;
	public ArrayList<Result> arrayExp;
	public int valueIfConstant;
	public int addressIfVariable;
	public Instruction instruction;
	public CODE cond;
}
