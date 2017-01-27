import Utilities.MyScanner;
import Utilities.ScannerUtils;

public class BasicScannerTesting {
	/**
	 * For Testing the basic scanner only
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		MyScanner sc = ScannerUtils.getScanner("D:\\Course Work\\ACC\\Project-Astoria\\Compiler\\src\\Test.txt");
		while(true) {
			sc.next();
			int token = sc.currentToken;
			if(token == ScannerUtils.eofToken) {
				System.out.println("EOF");
				break;
			}
			else if (token == ScannerUtils.number) {
				System.out.println("input was "+sc.val);
			} else if (token == ScannerUtils.ident) {
				System.out.println("input was "+sc.id);
			} else {
				System.out.println("input was something   "+sc.currentToken);
			}
		}
	}
}
