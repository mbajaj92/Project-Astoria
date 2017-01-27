import Utilities.Grammer;
import Utilities.MyScanner;
import Utilities.ScannerUtils;

public class Testing {
	private static MyScanner sc;

	public static void main(String args[]) throws Exception {
		sc = ScannerUtils.getScanner("D:\\Course Work\\ACC\\Project-Astoria\\Compiler\\src\\Test.txt");
		sc.next();
		Grammer.computation(sc);
		System.out.println("Parsing Complete");
	}
}