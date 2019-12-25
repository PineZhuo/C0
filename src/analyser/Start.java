package analyser;


import compiler.Pair;

public class Start {
	String opcode = new String();//指令名
	Pair operands;
	
	public Start(String opcode, Pair operands) {
		this.opcode = opcode;
		this.operands = operands;
	}
	
	public void print() {
		System.out.print(opcode + " ");
		if(operands.size() == 2) {
			System.out.println(operands.getFirst() + "," + operands.getSecond());
		}
		else if(operands.size() == 1) {
			System.out.println(operands.getFirst());
		}
		else {
			System.out.println();
		}
	}
	
	public void printBinary() {
		switch(opcode) {
			//这里只列出我用到的
			//ipush
			case "ipush":{
				System.out.print("02" + " ");
				printHexEight(operands.getFirst());
				break;
			}
			//loada
			case "loada":{
				System.out.print("0a" + " ");
				printHexFour(operands.getFirst());
				printHexEight(operands.getSecond());
				break;
			}
			//iload
			case "iload":{
				System.out.print("10" + " ");
				break;
			}
			//istore
			case "istore":{
				System.out.print("20" + " ");
				break;
			}
			//isub
			case "isub":{
				System.out.print("34" + " ");
				break;
			}
			//iadd
			case "iadd":{
				System.out.print("30" + " ");
				break;
			}
			//imul
			case "imul":{
				System.out.print("38" + " ");
				break;
			}
			//idiv
			case "idiv":{
				System.out.print("3c" + " ");
				break;
			}
			//ineg
			case "ineg":{
				System.out.print("40" + " ");
				break;
			}
			default:{
				break;
			}
		}
	}
	private void printHexFour(int num) {
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
	
	private void printHexEight(int num) {
		String s = Integer.toHexString(num);
		String s1 = new String();
		String s2 = new String();
		String s3 = new String();
		String s4 = new String();
		switch(s.length()) {
		case 1: {s1 = "00"; s2 = "00"; s3 = "00"; s4 = "0" + s; break;}
		case 2: {s1 = "00"; s2 = "00"; s3 = "00"; s4 = s; break;}
		case 3: {s1 = "00"; s2 = "00"; s3 = "0" + s.substring(0, 1); s4 = s.substring(1, 3); break;}
		case 4: {s1 = "00"; s2 = "00"; s3 = s.substring(0, 2); s4 = s.substring(2, 4); break;}
		case 5: {s1 = "00"; s2 = "0" + s.substring(0, 1); s3 = s.substring(1, 3); s4 = s.substring(3, 5); break;}
		case 6: {s1 = "00"; s2 = s.substring(0, 2); s3 = s.substring(2, 4); s4 = s.substring(4, 6); break;}
		case 7: {s1 = "0" + s.substring(0,1); s2 = s.substring(1, 3); s3 = s.substring(3, 5); s4 = s.substring(5, 7);break;}
		case 8: {s1 = s.substring(0,2); s2 = s.substring(2, 4); s3 = s.substring(4, 6); s4 = s.substring(6, 8);break;}
		default:break;
		}
		System.out.print(s1 + " " + s2 + " " + s3 + " " + s4 + " ");
	}
	
	
}
