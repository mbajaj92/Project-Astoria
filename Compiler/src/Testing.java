import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashSet;

import Utilities.BasicBlock;
import Utilities.Grammer;
import Utilities.Instruction;
import Utilities.MyScanner;
import Utilities.ScannerUtils;
import Utilities.Utils;

public class Testing {
	private static MyScanner sc;

	public static void main(String args[]) throws Exception {
		/*
		 * for (int i = 14; i <= 31; i++) { sc = ScannerUtils.
		 * getScanner("D:\\Course Work\\ACC\\Project-Astoria\\Compiler\\src\\Test Cases\\test"
		 * +i+".txt"); sc.next(); Grammer.computation(sc);
		 * Utils.SOPln("Parsing Complete for tes t" + i + ".txt");
		 * ScannerUtils.shutDown(); }
		 */
		sc = ScannerUtils.getScanner("Test.txt");
		sc.next();
		Grammer.computation(sc);
		Utils.printArrayTable();
		Utils.SOPln("Parsing Complete for Test" + /* i + */ ".txt\n\n\n");
		File f = new File("graph.dot");
		f.delete();
		f = new File("graph.png");
		f.delete();
		RandomAccessFile randomAccessFile = new RandomAccessFile("graph.dot", "rw");
		randomAccessFile.writeBytes("digraph {\n");
		for (BasicBlock i : BasicBlock.getBasicBlockList()) {
			if (i.shouldIgnore())
				continue;

			i.fixUp();
			Utils.SOPln(i);

			String write = i.getIndex() + "[label=\"" + i + "\n\"];\n" + i.getIndex() + "[shape=box];\n";
			if (i.firstChildExists())
				write += i.getIndex() + " -> " + i.getFirstChild().getIndex() + "[color=blue]\n";

			if (i.secondChildExists())
				write += i.getIndex() + " -> " + i.getSecondChild().getIndex() + "[color=black]\n";

			if (i.firstParentExists())
				write += i.getIndex() + " -> " + i.getFirstParent().getIndex() + "[color=red][style=dotted]\n";

			if (i.secondParentExists())
				write += i.getIndex() + " -> " + i.getSecondParent().getIndex() + "[color=green][style=dotted]\n";

			randomAccessFile.writeBytes(write);
		}
		randomAccessFile.writeBytes("}");
		randomAccessFile.close();

		Utils.SOPln("------");

		if (Instruction.getInstructionList() != null)
			for (Instruction i : Instruction.getInstructionList())
				Utils.SOPln(i.testToString());
		Runtime.getRuntime().exec("dot graph.dot -Tpng -o graph.png");

		Utils.SOPln("");
		Utils.SOPln("Basic Block Traversal");
		for (BasicBlock b : BasicBlock.getBasicBlockList()) {
			if (b.isLastBlock())
				Utils.traversefunc(b, new HashSet<Integer>());
		}

		//COLOR
		Utils.colorAndPrintInterfearenceGraph();
		ScannerUtils.shutDown();
	}
}