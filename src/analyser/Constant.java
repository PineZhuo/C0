package analyser;

//常量表的单元
public class Constant {
	private int index;
	private String type;
	private String stringValue;
	private int intValue;
	
	public Constant(int index, String type, String stringValue) {
		this.index = index;
		this.type = type;
		this.stringValue = stringValue;
	}
	
	public Constant(int index, String type, int intValue) {
		this.index = index;
		this.type = type;
		this.intValue = intValue;
	}
	
	public void print() {
		System.out.println(index + " " + type + " " + "\"" + stringValue + "\"");
	}
	
}
