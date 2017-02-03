package Utilities;

public class Instruction {
	int a, b ,c;
	CODE code;
	Instruction previous;
	enum CODE {
		ADD, ADDI, SUB, SUBI 
	};
}
