package Utilities;

import java.util.Arrays;
import java.util.List;

public class ScannerUtils {
	private static MyScanner myScanner = null;
	private static List<String> keywords = Arrays.asList("then", "do", "od", "fi", "else", "let", "call", "if",
			"while", "return", "var", "array", "function", "procedure", "main");

	public static MyScanner getCurrentScanner() throws Exception {
		if (myScanner == null)
			Utils.error("SCANNER NOT DEFINED");
		return myScanner;
	}

	public static MyScanner getScanner(String path) throws Exception {
		if (myScanner == null)
			myScanner = new MyScanner(path);
		return myScanner;
	}

	public static boolean isKeyword(String token) {
		return keywords.contains(token);
	}

	final public static int errorToken = 0;
	final public static int timesToken = 1; // *
	final public static int divToken = 2; ///

	final public static int plusToken = 11; // +
	final public static int minusToken = 12; // -
	final public static int eqlToken = 20; // ==
	final public static int neqToken = 21; // !=
	final public static int lssToken = 22; // <
	final public static int geqToken = 23; // >=
	final public static int leqToken = 24; // <=
	final public static int gtrToken = 25; // >

	final public static int periodToken = 30; // .
	final public static int commaToken = 31; // ,
	final public static int openbracketToken = 32; // [
	final public static int closebracketToken = 34; // ]
	final public static int closeparanToken = 35; // )

	final public static int becomesToken = 40; // <-
	final public static int thenToken = 41; // then
	final public static int doToken = 42; // do
	final public static int openparanToken = 50; // (

	final public static int number = 60;
	final public static int ident = 61;

	final public static int semiToken = 70; // ;

	final public static int endToken = 80; // }
	final public static int odToken = 81; // od
	final public static int fiToken = 82; // fi

	final public static int elseToken = 90; // else

	final public static int letToken = 100; // let
	final public static int callToken = 101; // call
	final public static int ifToken = 102; // if
	final public static int whileToken = 103; // while
	final public static int returnToken = 104; // return

	final public static int varToken = 110; // variable
	final public static int arrToken = 111; // array
	final public static int funcToken = 112; // function
	final public static int procToken = 113; // procedure

	final public static int beginToken = 150; // {
	final public static int mainToken = 200; // main
	final public static int eofToken = 255; // EOF

	public static void shutDown() {
		myScanner.shutDown();
		myScanner = null;
	}
}
