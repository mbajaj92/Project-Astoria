
public class Testing {
	public static void main(String args[]) throws Exception {
		MyScanner sc = new MyScanner("D:\\Course Work\\ACC\\Project-Astoria\\Compiler\\src\\Test.txt");
		while(true) {
			sc.next();
			int token = sc.sym;
			if(token == ScannerUtils.eofToken) {
				System.out.println("EOF");
				break;
			}
			else if (token == ScannerUtils.number) {
				System.out.println("input was "+sc.val);
			} else if (token == ScannerUtils.ident) {
				System.out.println("input was "+sc.id);
			} else {
				System.out.println("input was something   "+sc.sym);
			}
		}
	}
}
