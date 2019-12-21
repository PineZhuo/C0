package analyser;

import tokenizer.Token;

public class Symbol {
	String name;
	IdentiKind kind;
	IdentiType type;
	int level;
	
	public Symbol(Token token,IdentiType type, IdentiKind kind, int level) {
		name = token.getValue();
		this.kind = kind;
		this.type = type;
		this.level = level;
	}
	
	public int getLevel() {
		return level;
	}
	
	public String getName() {
		return name;
	}
	
	public IdentiKind getKind() {
		return kind;
	}
	
	public IdentiType getType() {
		return type;
	}
	
	public void printSymbol() {
		System.out.println("name:" + name + " "
					+ "kind:" + kind + " "
					+ "type:" + type + " "
					+ "level" + level);
	}
}
