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
	
	public void printBinary() {
		//因为type只可能是S，所以直接输出
		System.out.print("00 ");
		int len = stringValue.length();
		String s = Integer.toHexString(len);
		String s1 = new String();
		String s2 = new String();
		switch(s.length()) {
			case 1: {s1 = "00"; s2 = "0" + s; break;}
			case 2: {s1 = "00"; s2 = s; break;}
			case 3: {s1 = "0" + s.substring(0, 1); s2 = s.substring(1, 3); break;}
			case 4: {s1 = s.substring(0, 2); s2 = s.substring(2, 4); break;}
			default:{break;}
		}
		System.out.print(s1 + " " + s2 + " ");
		for(int i = 0; i < len; i++) {
			System.out.print(Integer.toHexString((int)stringValue.charAt(i)) + " ");
		}
	}
	
}
