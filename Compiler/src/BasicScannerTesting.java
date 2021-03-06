import Utilities.MyScanner;
import Utilities.ScannerUtils;
import Utilities.Utils;

public class BasicScannerTesting {
	/**
	 * For Testing the basic scanner only
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		MyScanner sc = ScannerUtils.getScanner("D:\\Course Work\\ACC\\Project-Astoria\\Compiler\\src\\Test.txt");
		while (true) {
			sc.next();
			int token = sc.currentToken;
			if (token == ScannerUtils.eofToken) {
				Utils.SOPln("EOF");
				break;
			} else if (token == ScannerUtils.number) {
				Utils.SOPln("input was " + sc.val);
			} else if (token == ScannerUtils.ident) {
				Utils.SOPln("input was " + sc.id);
			} else {
				Utils.SOPln("input was something   " + sc.currentToken);
			}
		}
	}
}
