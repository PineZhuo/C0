package tokenizer;
import compiler.Pair;
import error.ErrorType;

public class Token {
	private Pair pos;
	private TokenType type;
	private String value = new String();
	
	private ErrorType err;
	
	public Token(Pair pos, TokenType type, String value){
		this.pos = pos;
		this.type = type;
		this.value = value;
	}
	
	public Token(){
		
	}
	
	public Token(Pair pos, TokenType type, ErrorType err){
		this.pos = pos;
		this.type = type;
		this.err = err;
	}
	
	public void printToken() {
		if(type == TokenType.ERROR)
			System.out.println("line:" + pos.getFirst()
					+ " " + "column:" + pos.getSecond()
					+ " " + "ERROR!" + err);
		else
			System.out.println("line:" + pos.getFirst()
			+ " " + "column:" + pos.getSecond()
			+ " " + "type:" + type
			+ " " + "value:" + value);
	}
	
	public TokenType getTokenType() {
		return type;
	}
	public Pair getPos() {
		return pos;
	}
	
	public String getValue() {
		return value;
	}
//	public int getLine() {
//		return pos.getFirst();
//	}
//	
//	public int getCol() {
//		return pos.getSecond();
//	}
}
