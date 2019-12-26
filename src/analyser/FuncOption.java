package analyser;


import java.io.DataOutputStream;
import java.io.IOException;

import compiler.Pair;

public class FuncOption {
	String opcode = new String();//指令名
	Pair operands;
	
	public FuncOption(String opcode, Pair operands) {
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
	
	public String getOpcode() {
		return opcode;
	}
	
	public void setOperands(Pair op) {
		if(op.size() == 1) {
			operands = new Pair(op.getFirst());
		}
		else if(op.size() == 2) {
			operands = new Pair(op.getFirst(), op.getSecond());
		}
	}
	
	public int getOperandNum() {
		return operands.size();
	}
	
	public void printBinary(DataOutputStream out) throws IOException {
		switch(opcode) {
		//这里只列出我用到的
		//ipush
		case "ipush":{
			out.write(0x02);
			printHexEight(operands.getFirst(), out);
			break;
		}
		//loada
		case "loada":{
//			System.out.print("0a" + " ");
			out.write(0x0a);
			printHexFour(operands.getFirst(), out);
			printHexEight(operands.getSecond(), out);
			break;
		}
		case "loadc":{
			out.write(0x09);
			printHexFour(operands.getFirst(), out);
			break;
		}
		//iload
		case "iload":{
//			System.out.print("10" + " ");
			out.write(0x10);
			break;
		}
		//istore
		case "istore":{
//			System.out.print("20" + " ");
			out.write(0x20);
			break;
		}
		//isub
		case "isub":{
//			System.out.print("34" + " ");
			out.write(0x34);
			break;
		}
		//iadd
		case "iadd":{
//			System.out.print("30" + " ");
			out.write(0x30);
			break;
		}
		//imul
		case "imul":{
//			System.out.print("38" + " ");
			out.write(0x38);
			break;
		}
		//idiv
		case "idiv":{
//			System.out.print("3c" + " ");
			out.write(0x3c);
			break;
		}
		//ineg
		case "ineg":{
//			System.out.print("40" + " ");
			out.write(0x40);
			break;
		}
		//jmp
		case "jmp":{
//			System.out.print("70" + " " );
			out.write(0x70);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "je":{
//			System.out.print("71" + " ");
			out.write(0x71);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "jne":{
//			System.out.print("72" + " ");
			out.write(0x72);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "jl":{
//			System.out.print("73" + " ");
			out.write(0x73);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "jge":{
//			System.out.print("74" + " ");
			out.write(0x74);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "jg":{
//			System.out.print("75" + " ");
			out.write(0x75);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "jle":{
//			System.out.print("76" + " ");
			out.write(0x76);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "ret":{
//			System.out.print("88" + " ");
			out.write(0x88);
			break;
		}
		case "iret":{
//			System.out.print("89" + " ");
			out.write(0x89);
			break;
		}
		case "call":{
//			System.out.print("80" + " ");
			out.write(0x80);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "pop":{
//			System.out.print("04" + " ");
			out.write(0x04);
			break;
		}
		case "iscan":{
//			System.out.print("b0" + " ");
			out.write(0xb0);
			break;
		}
		case "iprint":{
//			System.out.print("a0" + " ");
			out.write(0xa0);
			break;
		}
		case "cprint":{
//			System.out.print("a2" + " ");
			out.write(0xa2);
			break;
		}
		case "bipush":{
//			System.out.print("01" + " "
//					+ Integer.toHexString(operands.getFirst(), out));
			out.write(0x01);
			out.write((byte)Integer.parseInt(Integer.toHexString(operands.getFirst()), 16));
			break;
		}
		case "printl":{
//			System.out.print("af" + " ");
			out.write(0xaf);
			break;
		}
		case "sprint":{
//			System.out.print("a3" + " ");
			out.write(0xa3);
			break;
		}
		default:{
			break;
		}
	}
		out.flush();
	}
	
	private void printHexFour(int num, DataOutputStream out) throws NumberFormatException, IOException {
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
		out.write((byte)Integer.parseInt(s1, 16));
		out.write((byte)Integer.parseInt(s2, 16));
		out.flush();
	}
	
	private void printHexEight(int num, DataOutputStream out) throws NumberFormatException, IOException {
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
//		System.out.print(s1 + " " + s2 + " " + s3 + " " + s4 + " ");
		out.write((byte)Integer.parseInt(s1, 16));
		out.write((byte)Integer.parseInt(s2, 16));
		out.write((byte)Integer.parseInt(s3, 16));
		out.write((byte)Integer.parseInt(s4, 16));
		out.flush();
	}
}
