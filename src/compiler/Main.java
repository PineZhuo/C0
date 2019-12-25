package compiler;

import java.io.IOException;

import analyser.Analyser;
import tokenizer.Tokenizer;

public class Main {

	public static void main(String[] args){
		Tokenizer tz = new Tokenizer();
		
		int len = args.length;
		String inputFile = new String();
		String outputFile = new String("out");
		if(len == 0) {
			System.out.println("Usage: cc0 [options] input [-o file]" + 
					" " +"请选择操作: \r\n"+
					"  -s        将输入的 c0 源代码翻译为文本汇编文件\r\n" + 
					"  -c        将输入的 c0 源代码翻译为二进制目标文件\r\n" +
					"  -h        显示关于编译器使用的帮助\r\n" + 
					"  -o file   输出到指定的文件 file");
			return;
		}
		else if(len == 1) {
			if(args[0].equals("-h")) {
				System.out.println("Usage: cc0 [options] input [-o file]" + 
						" " +"请选择操作: "+
						"  -s        将输入的 c0 源代码翻译为文本汇编文件\r\n" + 
						"  -h        显示关于编译器使用的帮助\r\n" + 
						"  -o file   输出到指定的文件 file");
				return;
			}
			else {
				System.out.println("invalid command");
				return ;
			}
		}
		else if(len == 2){
			if(args[0].equals("-s")) {
				inputFile = args[1];
			}
			else {
				System.out.println("invalid command");
				return ;
			}
		}
		else if(len == 4) {
			if((args[0].equals("-s") || args[0].equals("-c")) && args[2].equals("-o")) {
				inputFile = args[1];
				outputFile = args[3];
			}
			else {
				System.out.println("invalid command");
				return ;
			}
		}
		else {
			System.out.println("invalid command");
			return;
		}
//		
		Analyser as = new Analyser();
		try {
//			tz.runTokenizer();
//			as.runAnalyser("d://in.txt", "d://out.txt");
			if(args[0].equals("-s")) {//文本文件
				as.runAnalyser(inputFile, outputFile, 1);
			}
			else if(args[0].equals("-c")) {
				as.outputBinary(inputFile, outputFile);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
