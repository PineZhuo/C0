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
}
