package error;

import java.util.ArrayList;

import compiler.Pair;
import tokenizer.TokenType;

public class Error {
	private ErrorType type;
	private Pair pos = new Pair();
	private boolean isErr = true;
	private ArrayList<TokenType> typeList;
	private TokenType ExpriType;
	
	public Error(Pair pos, ErrorType type) {
		this.pos = pos;
		this.type = type;
	}
	
	public Error() {
		isErr = false;
	}
	
	@SuppressWarnings("unchecked")
	public Error(ArrayList<TokenType> typeList) {
		isErr = false;
		this.typeList = (ArrayList<TokenType>) typeList.clone();
	}
	public Error(TokenType ExpriType) {
		isErr = false;
		this.ExpriType = ExpriType;
	}
	
	public TokenType getTokenType() {
		return ExpriType;
	}
	
	public ArrayList<TokenType> getList(){
		return typeList;
	}
	
	public boolean isError() {
		return isErr;
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
