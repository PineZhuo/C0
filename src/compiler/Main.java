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
					" " +"��ѡ�����: \r\n"+
					"  -s        ������� c0 Դ���뷭��Ϊ�ı�����ļ�\r\n" + 
					"  -c        ������� c0 Դ���뷭��Ϊ������Ŀ���ļ�\r\n" +
					"  -h        ��ʾ���ڱ�����ʹ�õİ���\r\n" + 
					"  -o file   �����ָ�����ļ� file");
			return;
		}
		else if(len == 1) {
			if(args[0].equals("-h")) {
				System.out.println("Usage: cc0 [options] input [-o file]" + 
						" " +"��ѡ�����: "+
						"  -s        ������� c0 Դ���뷭��Ϊ�ı�����ļ�\r\n" + 
						"  -h        ��ʾ���ڱ�����ʹ�õİ���\r\n" + 
						"  -o file   �����ָ�����ļ� file");
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
			if(args[0].equals("-s")) {//�ı��ļ�
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
