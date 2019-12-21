package compiler;

import analyser.IdentiType;

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
