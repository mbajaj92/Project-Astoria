import Utilities.Grammer;
import Utilities.MyScanner;
import Utilities.ScannerUtils;

public class Testing {
	private static MyScanner sc;

	public static void main(String args[]) throws Exception {
		for (int i = 14; i <= 31; i++) {
			sc = ScannerUtils.getScanner("D:\\Course Work\\ACC\\Project-Astoria\\Compiler\\src\\Test Cases\\test"+i+".txt");
			sc.next();
			Grammer.computation(sc);
			System.out.println("Parsing Complete for test" + i + ".txt");
			ScannerUtils.shutDown();
		}
	}
}