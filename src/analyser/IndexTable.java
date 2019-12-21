package analyser;

import java.util.ArrayList;


import compiler.Pair;
import tokenizer.TokenType;

public class IndexTable {
	String funcName;
	IdentiType retType;
	ArrayList<Pair> paraList = new ArrayList<>();
	
	public IndexTable(String name, IdentiType retType, ArrayList<Pair> paraList) {
		funcName = name;
		this.retType = retType;
		this.paraList = paraList;
	}
	
	public String getName() {
		return funcName;
	}
	
	public int getParaNum() {
		return paraList.size();
	}
	
	public IdentiType getRetType() {
		return retType;
	}
}
