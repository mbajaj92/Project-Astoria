import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import Utilities.BasicBlock;
import Utilities.Grammer;
import Utilities.Instruction;
import Utilities.MyScanner;
import Utilities.ScannerUtils;
import Utilities.Utils;

public class Testing {
	private static MyScanner sc;

	public static void main(String args[]) throws Exception {
		// 27
		List<Integer> error = Arrays.asList(27, 34);
		for (int fileNumber/* :error */ = 0; fileNumber <= 0; fileNumber++) {
			Utils.SOPln("FILENUMBER = " + fileNumber);

			sc = ScannerUtils.getScanner(
					"D:\\Course Work\\ACC\\Project-Astoria\\Compiler\\Test Cases\\test" + fileNumber + ".txt");
			sc.next();
			Grammer.computation(sc);
			Utils.printArrayTable();
			Utils.SOPln("Parsing Complete for Test" + fileNumber + ".txt\n\n\n");
			Utils.SOPln("------");

			
			Utils.SOPln("");
			Utils.SOPln("Basic Block Traversal");
			for (BasicBlock b : BasicBlock.getBasicBlockList()) {
				if (b.isLastBlock())
					Utils.traversefunc(b, new HashSet<Integer>());
			}

			if (Instruction.getInstructionList() != null)
				for (Instruction i : Instruction.getInstructionList())
					Utils.SOPln(i.testToString());
			
			/* Graph Coloring */
			Utils.registerAllocation();

			File f = new File(fileNumber + "graph.dot");
			f.delete();
			f = new File(fileNumber + "graph.png");
			f.delete();
			RandomAccessFile randomAccessFile = new RandomAccessFile(fileNumber + "graph.dot", "rw");
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
			Runtime.getRuntime().exec("dot " + fileNumber + "graph.dot -Tpng -o " + fileNumber + "graph.png");

			f = new File(fileNumber + "iGraph.dot");
			f.delete();
			f = new File(fileNumber + "iGraph.png");
			f.delete();
			if (!Utils.getInterfearenceGraph().isEmpty()) {
				randomAccessFile = new RandomAccessFile(fileNumber + "iGraph.dot", "rw");
				randomAccessFile.writeBytes("strict graph {\n");
				for (Integer key : Utils.getInterfearenceGraph().keySet()) {
					// String write = key + "[label=\"" + key + "\" style=filled
					// fillcolor=\"red\"];\n";
					String write = key + "[label=\"" + key + "\" style=filled fillcolor=\""
							+ Instruction.getInstructionList().get(key).getColor() + "\"];\n";
					HashSet<Integer> edges = Utils.getInterfearenceGraph().get(key);

					for (int j : edges)
						write += key + " -- " + j + "\n";
					randomAccessFile.writeBytes(write);
				}
				randomAccessFile.writeBytes("}");
				randomAccessFile.close();
				Runtime.getRuntime().exec("dot " + fileNumber + "iGraph.dot -Tpng -o " + fileNumber + "iGraph.png");
			}

			f = new File(fileNumber + "phiGraph.dot");
			f.delete();
			f = new File(fileNumber + "phiGraph.png");
			f.delete();
			if (!Utils.getPhiClusters().isEmpty()) {
				randomAccessFile = new RandomAccessFile(fileNumber + "phiGraph.dot", "rw");
				randomAccessFile.writeBytes("strict graph {\n");
				for (Integer key : Utils.getPhiClusters().keySet()) {
					String write = key + "[label=\"" + key + "\" style=filled fillcolor=\""
							+ Instruction.getInstructionList().get(key).getColor() + "\"];\n";
					HashSet<Integer> edges = Utils.getPhiClusters().get(key);

					for (int j : edges)
						write += key + " -- " + j + "\n";
					randomAccessFile.writeBytes(write);
				}
				randomAccessFile.writeBytes("}");
				randomAccessFile.close();
				Runtime.getRuntime().exec("dot " + fileNumber + "phiGraph.dot -Tpng -o " + fileNumber + "phiGraph.png");
			}

			/* Still under development */
			Utils.SOPln("WE ARE STARTING MACHINE CODE");
			for (BasicBlock i : BasicBlock.getBasicBlockList()) {
				if (i.firstParentExists() || i.secondParentExists())
					continue;

				Utils.startLowering(i);
			}

			ScannerUtils.shutDown();
			Utils.shutDown();
		}
	}
}