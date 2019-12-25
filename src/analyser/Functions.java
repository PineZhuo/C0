package analyser;

public class Functions {
	int index;
	int name_index;
	int params_size;
	int level = 1;
	
	public Functions(int index, int name_index, int params_size) {
		this.index = index;
		this.name_index = name_index;
		this.params_size = params_size;
	}
	
	public void print() {
		System.out.println(index + " " + name_index + " " + params_size + " " + level);
	}
	
	public void printBinary() {
		printHexNum(name_index);
		printHexNum(params_size);
		printHexNum(level);
	}
	
	private void printHexNum(int num) {
		String s = Integer.toHexString(num);
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
	}
}
