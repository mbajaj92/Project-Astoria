import java.io.FileReader;
import java.util.Scanner;

public class Test {
	public static void main(String args[]) throws Exception {
		Scanner sc = new Scanner(new FileReader(args[0]));
		sc.useDelimiter("");
		while(sc.hasNext())
			System.out.println(sc.next());
	}
}
