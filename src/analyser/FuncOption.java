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
			out.write(0x10);
			break;
		}
		//istore
		case "istore":{
			out.write(0x20);
			break;
		}
		//isub
		case "isub":{
			out.write(0x34);
			break;
		}
		//iadd
		case "iadd":{
			out.write(0x30);
			break;
		}
		//imul
		case "imul":{
			out.write(0x38);
			break;
		}
		//idiv
		case "idiv":{
			out.write(0x3c);
			break;
		}
		//ineg
		case "ineg":{
			out.write(0x40);
			break;
		}
		//jmp
		case "jmp":{
			out.write(0x70);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "je":{
			out.write(0x71);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "jne":{
			out.write(0x72);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "jl":{
			out.write(0x73);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "jge":{
			out.write(0x74);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "jg":{
			out.write(0x75);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "jle":{
			out.write(0x76);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "ret":{
			out.write(0x88);
			break;
		}
		case "iret":{
			out.write(0x89);
			break;
		}
		case "call":{
			out.write(0x80);
			printHexFour(operands.getFirst(), out);
			break;
		}
		case "pop":{
			out.write(0x04);
			break;
		}
		case "iscan":{
			out.write(0xb0);
			break;
		}
		case "iprint":{
			out.write(0xa0);
			break;
		}
		case "cprint":{
			out.write(0xa2);
			break;
		}
		case "bipush":{
			out.write(0x01);
			out.write((byte)Integer.parseInt(Integer.toHexString(operands.getFirst()), 16));
			break;
		}
		case "printl":{
			out.write(0xaf);
			break;
		}
		case "sprint":{
			out.write(0xa3);
			break;
		}
		case "dload":{
			out.write(0x11);
			break;
		}
		case "snew":{
			out.write(0x0c);
			printHexEight(operands.getFirst(), out);
			break;
		}
		case "dret":{
			out.write(0x8a);
			break;
		}
		case "nop":{
			out.write(0x00);
			break;
		}
		case "dscan":{
			out.write(0xb1);
			break;
		}
		case "dstore":{
			out.write(0x21);
			break;
		}
		case "cscan":{
			out.write(0xb2);
			break;
		}
		case "dprint":{
			out.write(0xa1);
			break;
		}
		case "i2d":{
			out.write(0x60);
			break;
		}
		case "d2i":{
			out.write(0x61);
			break;
		}
		case "i2c":{
			out.write(0x62);
			break;
		}
		case "icmp":{
			out.write(0x44);
			break;
		}
		case "dcmp":{
			out.write(0x45);
			break;
		}
		case "dadd":{
			out.write(0x31);
			break;
		}
		case "dsub":{
			out.write(0x35);
			break;
		}
		case "dmul":{
			out.write(0x39);
			break;
		}
		case "ddiv":{
			out.write(0x3d);
			break;
		}
		case "dneg":{
			out.write(0x41);
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
