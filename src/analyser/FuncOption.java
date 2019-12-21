package analyser;


import compiler.Pair;

public class FuncOption {
	String opcode = new String();//÷∏¡Ó√˚
	Pair operands;
	
	public FuncOption(String opcode, Pair operands) {
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
	
	public String getOpcode() {
		return opcode;
	}
	
	public void setOperands(Pair op) {
		if(op.size() == 1) {
			operands = new Pair(op.getFirst());
		}
		else if(op.size() == 2) {
			operands = new Pair(op.getFirst(), op.getSecond());
		}
	}
	
	public int getOperandNum() {
		return operands.size();
	}
}
