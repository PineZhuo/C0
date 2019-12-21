package error;

import compiler.Pair;

public class Error {
	ErrorType type;
	int line;
	int col;
	Pair pos = new Pair();
	
	public Error(Pair pos, ErrorType type) {
		this.pos = pos;
		this.type = type;
	}
	
	public ErrorType getErrorType() {
		return type;
	}
	
	public void printError() {
		System.out.println("line:" + pos.getFirst() + " "
					+ "column:" + pos.getSecond() + " "
					+ "type:" + type);
	}
}
