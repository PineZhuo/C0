package compiler;

import analyser.IdentiType;
import tokenizer.TokenType;

public class Pair {
	private int first;
	private int second;
	private String name;
	private IdentiType type;
	private int size;
	
	public Pair(int first, int second) {
		this.first = first;
		this.second = second;
		size = 2;
	}
	
	public Pair() {
		size = 0;
	}
	
	public Pair(String name, IdentiType type) {
		this.name = name;
		this.type = type;
		size = 2;
	}
	
	public Pair(int first) {
		this.first = first;
		size = 1;
	}
	
	public String getName() {
		return name;
	}
	
	public IdentiType getType() {
		return type;
	}
	
	public TokenType getTokenType() {
		if(type == IdentiType.CHAR || type == IdentiType.CONST_CHAR)
			return TokenType.CHAR;
		else if(type == IdentiType.INT || type == IdentiType.CONST_INT)
			return TokenType.INT;
		else if(type == IdentiType.DOUBLE || type == IdentiType.CONST_DOUBLE)
			return TokenType.DOUBLE;
		return null;
	}
	
	public int getFirst() {
		return first;
	}
	
	public int getSecond() {
		return second;
	}
	
	public void setPair(int first, int second) {
		this.first = first;
		this.second = second;
	}
	
	public void setPair(int first) {
		this.first = first;
	}
	
	public int size() {
		return size;
	}
}
