package analyser;


import compiler.Pair;

public class Start {
	String opcode = new String();//÷∏¡Ó√˚
	Pair operands;
	
	public Start(String opcode, Pair operands) {
		this.opcode = opcode;
		this.operands = operands;
	}
	
	public void print() {
		System.out.print(opcode + " ");
		if(operands.size() == 2) {
			System.out.println(operands.getFirst() + "," + operands.getSecond());
		}
		else if(operands.size() == 1) {
			System.out.println(operands.getFirst());
		}
		else {
			System.out.println();
		}
	}
	
}
