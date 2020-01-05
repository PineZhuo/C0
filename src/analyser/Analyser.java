package analyser;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import compiler.Pair;
import error.Error;
import error.ErrorType;
import tokenizer.Token;
import tokenizer.TokenType;
import tokenizer.Tokenizer;

public class Analyser {
	
	private Tokenizer tz = new Tokenizer();
	//从词法分析拿到的tokenList的下标
	private int index = 0;
	//词法分析的结果
	private ArrayList<Token> tokenList = new ArrayList<>();
//	private int tokenNum;
	//函数的个数
	private int funcNum = 0;
	//构建索引表
	private ArrayList<IndexTable> indexTable = new ArrayList<>();
	private ArrayList<Pair> paraList = new ArrayList<>();
	//遇到一个大括号就加一层
	private int level = 0;
	//全局的栈式符号表
	private Stack<Symbol> symbolTable = new Stack<Symbol>();
	//一个用来计算函数调用时参数个数的全局变量
	private int paraNum = 0;
	//常量表的下标
	private int constIndex = 0;
	private ArrayList<Constant> constTable = new ArrayList<>();
	private ArrayList<Start> startTable = new ArrayList<>();
	private ArrayList<Functions> funcTable = new ArrayList<>();
	private ArrayList<ArrayList<FuncOption> > funcOpTable = new ArrayList<>();
	private TokenType retType;//记录当前函数声明返回值
	ArrayList<TokenType> typeList = new ArrayList<>();//记录强制转换的类型列表
//	private Vector<FuncOption> nowFuncTable = new Vector<>();
//	private int funcIndex = 0;
	
	File file;
	FileOutputStream fileOutputStream;
	PrintStream printStream;
	
	
	public void runAnalyser(String in, String out, int fileType) throws IOException  {
		tokenList = tz.getTokenList(in);
		if(tokenList == null) //说明词法分析出错了
			return ;
		
		Error err = Program();
		if(err.isError()) {
			err.printError();
			return ;
		}
			
		if(!isHaveMain()) {
			System.out.println("Error! There is not main function!");
			return ;
		}
		
		if(fileType == 1) {
			//控制台输出改到文件中
//			file = new File(out);
//			fileOutputStream = new FileOutputStream(file);
//			printStream = new PrintStream(fileOutputStream);
//			System.setOut(printStream);
			System.out.println(".constants:");
			for(int i = 0; i < constTable.size(); i++) {
				constTable.get(i).print();
			}
			System.out.println(".start:");
			for(int i = 0; i < startTable.size(); i++) {
				System.out.print(i + " ");
				startTable.get(i).print();
			}
			System.out.println(".functions:");
			for(int i = 0; i < funcTable.size(); i++) {
				funcTable.get(i).print();
			}
//			System.out.println(funcOpTable.get(0));
			for(int i = 0; i < funcNum; i++) {
				System.out.println(".F" + i + ":");
				for(int j = 0; j < funcOpTable.get(i).size(); j++) {
					System.out.print(j + " ");
					funcOpTable.get(i).get(j).print();
				}
			}
		}
	}
	
	public void outputBinary(String in, String outFile) throws IOException {
		runAnalyser(in, outFile, 2);
		//控制台输出改到文件中
		file = new File(outFile);
//		fileOutputStream = new FileOutputStream(file);
//		printStream = new PrintStream(fileOutputStream);
//		System.setOut(printStream);
//		System.out.print("");
		//magic
//		System.out.print("43 30 3a 29 ");
//		FileOutputStream out1 = new FileOutputStream(new FileOutputStream(file, true));

//		FileOutputStream out = new FileOutputStream(file);
//		byte[] b1 = {0x43, 0x30, 0x3a, 0x29};
//		for(int i = 0; i < 4; i++) {
//			out.write(b1[i]);
//			out.write(0x32);
//		}
//		out.flush();
		DataOutputStream out = new DataOutputStream(new FileOutputStream(outFile, false));
        byte[] bytes = {0x43, 0x30, 0x3a, 0x29};
//        	int a1 = 4096;
//        	byte[] r = new byte[4];
//        	String s = Integer.toHexString(a1);
        for(int i = 0; i < 4; i++) {
//        		out.write((byte)Integer.parseInt(s.substring(j,j+2), 16));
        	out.write((byte)bytes[i]);
        	out.flush();
        }
        	
//		}
		//version
        byte[] byte2 = {0x00, 0x00, 0x00, 0x01};
        for(int i = 0; i < 4; i++) {
        	out.write((byte)byte2[i]);
        	out.flush();
        }
//		//constants_count
		printHexCount(constTable.size(), out);
//		//constants
		for(int i = 0; i < constTable.size(); i++) {
			constTable.get(i).printBinary(out);
		}
//		//start_code
//		//instructions_count
		printHexCount(startTable.size(), out);
//		//instructions
		for(int i = 0; i < startTable.size(); i++) {
			startTable.get(i).printBinary(out);
		}
//		//functions_count
		int a = funcTable.size();
		printHexCount(a, out);
		for(int i = 0; i < a; i++) {
			funcTable.get(i).printBinary(out);
			//instructions_count
			int b = funcOpTable.get(i).size();
			printHexCount(b, out);
			for(int j = 0; j < b; j++) {
				funcOpTable.get(i).get(j).printBinary(out);
			}
		}
		out.close();
		
	}
	
	private void printHexCount(int count, DataOutputStream out) throws NumberFormatException, IOException {
		String s = Integer.toHexString(count);
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
	
	// <程序> -> {变量声明}{函数定义}
	// 返回空表示没有错误
	private Error Program() {
		while(isVariableHead()) {
			Error err = varDec();
			if(err.isError()) return err;
		}
		while(isFuncHead()) {
			Error err = funcDef();
			if(err.isError()) return err;
		}
		return new Error();
	}
	// <变量声明> -> [const] <类型><初始化说明列表>';'
	// int a , b = 1;
	private Error varDec() {
		boolean isConst = false;
//		char type;
		Token token = nextToken();
		if(token.getTokenType() == TokenType.CONST) {
			token = nextToken();
			isConst = true;
		}
		
		if(token.getTokenType() != TokenType.INT 
				&& token.getTokenType() != TokenType.CHAR
				&& token.getTokenType() != TokenType.DOUBLE) {
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		}
		TokenType tt = token.getTokenType();
		
		Error err = initDecList(isConst, tt);
		if(err.isError()) return err;
		
		token = nextToken();
		if(token.getTokenType() != TokenType.SEMICOLON)
			return new Error(token.getPos(), ErrorType.NO_SEMICOLON_ERROR);
		return new Error();
	}
	
	
	//<初始化说明列表> -> <初始化说明>{','<初始化说明>}
	// a, b, c = 1
	private Error initDecList(boolean isConst, TokenType tt)  {
		Error err = initDec(isConst, tt);
		if(err.isError()) return err;
		Token token = nextToken();
		while(token.getTokenType() == TokenType.COMMA) {
			err = initDec(isConst, tt);
			if(err.isError()) return err;
			token = nextToken();
		}
		unreadToken();
		return new Error();
	}
	
	//<初始化说明> -> <标识符> [<初始化>]
	// a
	// b = 1
	private Error initDec(boolean isConst, TokenType tt)  {
		Token token = nextToken();
		if(token.getTokenType() == TokenType.IDENTIFIER) {
			//此处构建符号表
			//如果重复命名 报错
			if(isSecondName(token.getValue())) {
				return new Error(token.getPos(), ErrorType.DUPLICATE_DECLARATION);
			}
//			String name = token.getValue();
			
			if(isConst) {
				if(tt == TokenType.INT)
					symbolTable.push(new Symbol(token, IdentiType.CONST_INT, IdentiKind.VARIABLE, level));
				else if(tt == TokenType.CHAR)
					symbolTable.push(new Symbol(token, IdentiType.CONST_CHAR, IdentiKind.VARIABLE, level));
				else if(tt == TokenType.DOUBLE)
					symbolTable.push(new Symbol(token, IdentiType.CONST_DOUBLE, IdentiKind.VARIABLE, level));
			}
			else {
				if(tt == TokenType.INT)
					symbolTable.push(new Symbol(token, IdentiType.INT, IdentiKind.VARIABLE, level));
				else if(tt == TokenType.CHAR)
					symbolTable.push(new Symbol(token, IdentiType.CHAR, IdentiKind.VARIABLE, level));
				else if(tt == TokenType.DOUBLE)
					symbolTable.push(new Symbol(token, IdentiType.DOUBLE, IdentiKind.VARIABLE, level));
			}
			
			token = nextToken();
			
			//常量未赋初值
			if(isConst && token.getTokenType() != TokenType.EQUAL_SIGN) {
				return new Error(token.getPos(), ErrorType.CONSTANT_NEED_VALUE);
			}
			
			//常量或者变量被赋初值
			if(token.getTokenType() == TokenType.EQUAL_SIGN) {
				unreadToken();
				Error err = init(tt);
				if(err.isError()) return err;
			}
			else {
				//赋初值0
				if(level == 0) {
					if(tt == TokenType.INT)
						startTable.add(new Start("ipush", new Pair(0)));
					else if(tt == TokenType.CHAR)
						startTable.add(new Start("bipush", new Pair(0)));
					else if(tt == TokenType.DOUBLE) {
						constTable.add(new Constant(constIndex, "D", 0.0));
						startTable.add(new Start("loadc", new Pair(constIndex)));
						constIndex++;
					}
				}
				else {
					if(tt == TokenType.INT)
						funcOpTable.get(funcNum-1).add(new FuncOption("ipush", new Pair(0)));
					else if(tt == TokenType.CHAR)
						funcOpTable.get(funcNum-1).add(new FuncOption("bipush", new Pair(0)));
					else if(tt == TokenType.DOUBLE) {
						constTable.add(new Constant(constIndex, "D", 0.0));
						funcOpTable.get(funcNum-1).add(new FuncOption("loadc", new Pair(constIndex)));
						constIndex++;
					}
				}
				
				unreadToken();
			}
		}
		else {
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		}
		return new Error();
	}
	
	//<初始化> -> ‘=’ <表达式>
	// = 1+9
	private Error init(TokenType tt)  {
		Token token = nextToken();
		if(token.getTokenType() == TokenType.EQUAL_SIGN) {
			Error err = expression();
			if(err.isError()) return err;
			TokenType right = err.getTokenType();
			if(tt == TokenType.CHAR) {
				if(right == TokenType.INT) {
					if(level == 0) {
						startTable.add(new Start("i2c", new Pair()));
					}
					else {
						funcOpTable.get(funcNum-1).add(new FuncOption("i2c", new Pair()));
					}
				}
				else if(right == TokenType.DOUBLE) {
					if(level == 0) {
						startTable.add(new Start("d2i", new Pair()));
						startTable.add(new Start("i2c", new Pair()));
					}
					else {
						funcOpTable.get(funcNum-1).add(new FuncOption("d2i", new Pair()));
						funcOpTable.get(funcNum-1).add(new FuncOption("i2c", new Pair()));
					}
				}
			}
			else if(tt == TokenType.INT) {
				if(right == TokenType.DOUBLE) {
					if(level == 0) {
						startTable.add(new Start("d2i", new Pair()));
					}
					else {
						funcOpTable.get(funcNum-1).add(new FuncOption("d2i", new Pair()));
					}
				}
			}
			else if(tt == TokenType.DOUBLE) {
				if(right == TokenType.INT || right == TokenType.CHAR) {
					if(level == 0) {
						startTable.add(new Start("i2d", new Pair()));
					}
					else {
						funcOpTable.get(funcNum-1).add(new FuncOption("i2d", new Pair()));
					}
				}
			}
		}
		else
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		return new Error();
	}
	
	// <赋值表达式> -> <标识符>[ '=' <表达式>]
	private Error assignExpre() {
		Token token = nextToken();
		if(token.getTokenType() != TokenType.IDENTIFIER)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		String name = token.getValue();
		
		//有等号需要比没有等号多一个条件->不是常量
		if(!isAbleToAssign1(name))
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		token = nextToken();
		if(token.getTokenType() == TokenType.EQUAL_SIGN) {
			//有等号 考虑是不是常量
			TokenType tt = isAbleToAssign2(name);
			if(tt == null) {
				return new Error(getLastToken().getPos(), ErrorType.INVALID_ASSIGNMENT);
			}
			//根据name找到在栈中的偏移
			Pair p1 = getLevelandIndex(name);
			if(level == 0) {
				startTable.add(new Start("loada", p1));
			}else {
				funcOpTable.get(funcNum-1).add(new FuncOption("loada", p1));
			}
			Error err = expression();
			if(err.isError()) return err;
			transType(err.getTokenType(), tt);
			if(level == 0) {
				if(tt == TokenType.CHAR || tt == TokenType.INT)
					startTable.add(new Start("istore", new Pair()));
				else if(tt == TokenType.DOUBLE)
					startTable.add(new Start("dstore", new Pair()));
			}
			else {
				if(tt == TokenType.CHAR || tt == TokenType.INT)
					funcOpTable.get(funcNum-1).add(new FuncOption("istore", new Pair()));
				else if(tt == TokenType.DOUBLE)
					funcOpTable.get(funcNum-1).add(new FuncOption("dstore", new Pair()));
			}
		}
		else
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		return new Error();
	}
	
	// <状态> -> <表达式>[<比较符号> <表达式>]
	private Error condition()  {
		Error err = expression();
		if(err.isError()) return err;
		TokenType left = err.getTokenType();
		funcOpTable.get(funcNum-1).add(new FuncOption("nop", new Pair()));
		int offset = funcOpTable.get(funcNum-1).size()-1;
		Token token = nextToken();
		if(isCompareSign(token)) {
			String compare = token.getValue();
			err = expression();
			if(err.isError()) return err;
			TokenType right = err.getTokenType();
			if(left == right) {
				if(left == TokenType.CHAR || left == TokenType.INT) {
					funcOpTable.get(funcNum-1).add(new FuncOption("icmp", new Pair()));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("dcmp", new Pair()));
				}
			}
			else {
				if(left == TokenType.DOUBLE) {
					funcOpTable.get(funcNum-1).add(new FuncOption("i2d", new Pair()));
					funcOpTable.get(funcNum-1).add(new FuncOption("dcmp", new Pair()));
				}
				else if(left == TokenType.INT || left == TokenType.CHAR) {
					if(right == TokenType.CHAR || right == TokenType.INT) {
						funcOpTable.get(funcNum-1).add(new FuncOption("icmp", new Pair()));
					}
					else {
						funcOpTable.get(funcNum-1).set(offset, new FuncOption("i2d", new Pair()));
						funcOpTable.get(funcNum-1).add(new FuncOption("dcmp", new Pair()));
					}
				}
			}
//			funcOpTable.get(funcNum-1).add(new FuncOption("isub", new Pair()));
			addCompareInstruct(compare);
		}
		else {
			int a1 = funcOpTable.get(funcNum-1).size() + 2;
			funcOpTable.get(funcNum-1).add(new FuncOption("jne", new Pair(a1)));
			funcOpTable.get(funcNum-1).add(new FuncOption("jmp", new Pair()));
			unreadToken();
		}
		return new Error();
	}
	
	// <表达式> -> <表达式语句>
	private Error expression()  {
		Error err = addExpre();
		if(err.isError()) return err;
//		System.out.println(err.getTokenType());
		return new Error(err.getTokenType());
	}
	
	// <表达式语句> -> <项> { <加减符号> <项>}
	private Error addExpre()  {
		Error err = mulExpre();
		if(err.isError()) return err;
		TokenType tt1 = err.getTokenType();
		Token token = nextToken();
		while(token.getTokenType() == TokenType.PLUS_SIGN 
				|| token.getTokenType() == TokenType.MINUS_SIGN) {
			int offset;
			if(level == 0) {
				startTable.add(new Start("nop", new Pair()));//无需转换类型
				offset = startTable.size()-1;
			}
			else {
				funcOpTable.get(funcNum-1).add(new FuncOption("nop", new Pair()));
				offset = funcOpTable.get(funcNum-1).size()-1;
			}
			err = mulExpre();
			if(err.isError()) return err;
			TokenType tt2 = err.getTokenType();
			 
			if(token.getTokenType() == TokenType.PLUS_SIGN) {
				if(level == 0) {
					if(tt1 == TokenType.INT || tt1 == TokenType.CHAR) {
						if(tt2 == TokenType.CHAR || tt2 == TokenType.INT) {
							startTable.add(new Start("iadd", new Pair()));//无需转换类型
							tt1 = TokenType.INT;
						}
						else if(tt2 == TokenType.DOUBLE) {
							startTable.set(offset, new Start("i2d", new Pair()));
							startTable.add(new Start("dadd", new Pair()));
							tt1 = TokenType.DOUBLE;
						}
					}
					else if(tt1 == TokenType.DOUBLE) {	
						if(tt2 == TokenType.DOUBLE) {
							startTable.add(new Start("dadd", new Pair()));
						}
						else {
							startTable.add(new Start("i2d", new Pair()));
							startTable.add(new Start("dadd", new Pair()));
						}
					}
				}
				else {
					if(tt1 == TokenType.INT) {
						if(tt2 == TokenType.CHAR || tt2 == TokenType.INT) {
							funcOpTable.get(funcNum-1).add(new FuncOption("iadd", new Pair()));//无需转换类型
							tt1 = TokenType.INT;
						}
						else if(tt2 == TokenType.DOUBLE) {
							funcOpTable.get(funcNum-1).set(offset, new FuncOption("i2d", new Pair()));
							funcOpTable.get(funcNum-1).add(new FuncOption("dadd", new Pair()));
							tt1 = TokenType.DOUBLE;
						}
					}
					else if(tt1 == TokenType.DOUBLE) {	
						if(tt2 == TokenType.DOUBLE) {
							funcOpTable.get(funcNum-1).add(new FuncOption("dadd", new Pair()));
						}
						else {
							funcOpTable.get(funcNum-1).add(new FuncOption("i2d", new Pair()));
							funcOpTable.get(funcNum-1).add(new FuncOption("dadd", new Pair()));
						}
					}
					
				}
			}
			else {
				if(level == 0) {
					if(tt1 == TokenType.INT || tt1 == TokenType.CHAR) {
						if(tt2 == TokenType.CHAR || tt2 == TokenType.INT) {
							startTable.add(new Start("isub", new Pair()));//无需转换类型
							tt1 = TokenType.INT;
						}
						else if(tt2 == TokenType.DOUBLE) {
							startTable.set(offset, new Start("i2d", new Pair()));
							startTable.add(new Start("dsub", new Pair()));
							tt1 = TokenType.DOUBLE;
						}
					}
					else if(tt1 == TokenType.DOUBLE) {	
						if(tt2 == TokenType.DOUBLE) {
							startTable.add(new Start("dsub", new Pair()));
						}
						else {
							startTable.add(new Start("i2d", new Pair()));
							startTable.add(new Start("dsub", new Pair()));
						}
					}
				}
				else {
					if(tt1 == TokenType.INT) {
						if(tt2 == TokenType.CHAR || tt2 == TokenType.INT) {
							funcOpTable.get(funcNum-1).add(new FuncOption("isub", new Pair()));//无需转换类型
							tt1 = TokenType.INT;
						}
						else if(tt2 == TokenType.DOUBLE) {
							funcOpTable.get(funcNum-1).set(offset, new FuncOption("i2d", new Pair()));
							funcOpTable.get(funcNum-1).add(new FuncOption("dsub", new Pair()));
							tt1 = TokenType.DOUBLE;
						}
					}
					else if(tt1 == TokenType.DOUBLE) {	
						if(tt2 == TokenType.DOUBLE) {
							funcOpTable.get(funcNum-1).add(new FuncOption("dsub", new Pair()));
						}
						else {
							funcOpTable.get(funcNum-1).add(new FuncOption("i2d", new Pair()));
							funcOpTable.get(funcNum-1).add(new FuncOption("dsub", new Pair()));
						}
					}
					
				}
			}
			token = nextToken();
		}
		unreadToken();
		return new Error(tt1);
	}
	
	// <项> -> <cast表达式> { <乘除符号> <cast表达式>}
	
	private Error mulExpre()  {
		Error err = castExpre();
		if(err.isError()) return err;
		TokenType tt1 = err.getTokenType();
		
		Token token = nextToken();
		
		while(token.getTokenType() == TokenType.MUL_SIGN
				|| token.getTokenType() == TokenType.DIV_SIGN) {
			int offset;
			if(level == 0) {
				startTable.add(new Start("nop", new Pair()));//无需转换类型
				offset = startTable.size()-1;
			}
			else {
				funcOpTable.get(funcNum-1).add(new FuncOption("dmul", new Pair()));
				offset = funcOpTable.get(funcNum-1).size()-1;
			}
			
			err = castExpre();
			if(err.isError()) return err;
			TokenType tt2 = err.getTokenType();
			if(token.getTokenType() == TokenType.MUL_SIGN) {
				if(level == 0) {
					if(tt1 == TokenType.INT || tt1 == TokenType.CHAR) {
						if(tt2 == TokenType.CHAR || tt2 == TokenType.INT) {
							startTable.add(new Start("imul", new Pair()));//无需转换类型
							tt1 = TokenType.INT;
						}
						else if(tt2 == TokenType.DOUBLE) {
							startTable.set(offset, new Start("i2d", new Pair()));
							startTable.add(new Start("dmul", new Pair()));
							tt1 = TokenType.DOUBLE;
						}
					}
					else if(tt1 == TokenType.DOUBLE) {	
						if(tt2 == TokenType.DOUBLE) {
							startTable.add(new Start("dmul", new Pair()));
						}
						else {
							startTable.add(new Start("i2d", new Pair()));
							startTable.add(new Start("dmul", new Pair()));
						}
					}
				}
				else {
					if(tt1 == TokenType.INT) {
						if(tt2 == TokenType.CHAR || tt2 == TokenType.INT) {
							funcOpTable.get(funcNum-1).add(new FuncOption("imul", new Pair()));//无需转换类型
							tt1 = TokenType.INT;
						}
						else if(tt2 == TokenType.DOUBLE) {
							funcOpTable.get(funcNum-1).set(offset, new FuncOption("i2d", new Pair()));
							funcOpTable.get(funcNum-1).add(new FuncOption("dmul", new Pair()));
							tt1 = TokenType.DOUBLE;
						}
					}
					else if(tt1 == TokenType.DOUBLE) {	
						if(tt2 == TokenType.DOUBLE) {
							funcOpTable.get(funcNum-1).add(new FuncOption("dmul", new Pair()));
						}
						else {
							funcOpTable.get(funcNum-1).add(new FuncOption("i2d", new Pair()));
							funcOpTable.get(funcNum-1).add(new FuncOption("dmul", new Pair()));
						}
					}
					
				}
			}
			else {
				if(level == 0) {
					if(tt1 == TokenType.INT || tt1 == TokenType.CHAR) {
						if(tt2 == TokenType.CHAR || tt2 == TokenType.INT) {
							startTable.add(new Start("idiv", new Pair()));//无需转换类型
							tt1 = TokenType.INT;
						}
						else if(tt2 == TokenType.DOUBLE) {
							startTable.set(offset, new Start("i2d", new Pair()));
							startTable.add(new Start("ddiv", new Pair()));
							tt1 = TokenType.DOUBLE;
						}
					}
					else if(tt1 == TokenType.DOUBLE) {	
						if(tt2 == TokenType.DOUBLE) {
							startTable.add(new Start("ddiv", new Pair()));
						}
						else {
							startTable.add(new Start("i2d", new Pair()));
							startTable.add(new Start("ddiv", new Pair()));
						}
					}
				}
				else {
					if(tt1 == TokenType.INT) {
						if(tt2 == TokenType.CHAR || tt2 == TokenType.INT) {
							funcOpTable.get(funcNum-1).add(new FuncOption("idiv", new Pair()));//无需转换类型
							tt1 = TokenType.INT;
						}
						else if(tt2 == TokenType.DOUBLE) {
							funcOpTable.get(funcNum-1).set(offset, new FuncOption("i2d", new Pair()));
							funcOpTable.get(funcNum-1).add(new FuncOption("ddiv", new Pair()));
							tt1 = TokenType.DOUBLE;
						}
					}
					else if(tt1 == TokenType.DOUBLE) {	
						if(tt2 == TokenType.DOUBLE) {
							funcOpTable.get(funcNum-1).add(new FuncOption("ddiv", new Pair()));
						}
						else {
							funcOpTable.get(funcNum-1).add(new FuncOption("i2d", new Pair()));
							funcOpTable.get(funcNum-1).add(new FuncOption("ddiv", new Pair()));
						}
					}
					
				}
			}
			token = nextToken();
		}
		unreadToken();//之前多读了一个不是＋和-的，回退
		return new Error(tt1);
	}
	
	
	// <cast表达式> -> {'('<类型>')'}<一元表达式>
	private Error castExpre() {
		Token token = nextToken();
		ArrayList<TokenType> typeList = new ArrayList<>();
		while(token.getTokenType() == TokenType.LEFT_BRACKET) {
			token = nextToken();
			if(token.getTokenType() == TokenType.DOUBLE
					|| token.getTokenType() == TokenType.INT
					|| token.getTokenType() == TokenType.CHAR) {
				typeList.add(token.getTokenType());
			}
			else {
				unreadToken();
				break;
			}
			token = nextToken();
			if(token.getTokenType() != TokenType.RIGHT_BRACKET) {
				return new Error(token.getPos(), ErrorType.NO_RIGHT_BRACKET);
			}
			token = nextToken();
		}
		unreadToken();
		Error err = unaryExpre();
		if(err.isError()) return err;
		TokenType tt1 = err.getTokenType();
//		System.out.println(tt1);
		int len = typeList.size();
		for(int i = len-1; i >= 0; i--) {
			TokenType tt2 = typeList.get(i);
			if(tt2 == TokenType.CHAR) {
				if(tt1 == TokenType.DOUBLE) {
					if(level == 0) {
						startTable.add(new Start("d2i", new Pair()));
						startTable.add(new Start("i2c", new Pair()));
					}
					else {
						funcOpTable.get(funcNum-1).add(new FuncOption("d2i", new Pair()));
						funcOpTable.get(funcNum-1).add(new FuncOption("i2c", new Pair()));
					}
					tt1 = TokenType.CHAR;
				}
				else if(tt1 == TokenType.INT) {
					if(level == 0) {
						startTable.add(new Start("i2c", new Pair()));
					}
					else {
						funcOpTable.get(funcNum-1).add(new FuncOption("i2c", new Pair()));
					}
					tt1 = TokenType.CHAR;
				}
			}
			else if(tt2 == TokenType.INT) {
				if(tt1 == TokenType.DOUBLE) {
					if(level == 0) {
						startTable.add(new Start("d2i", new Pair()));
					}
					else {
						funcOpTable.get(funcNum-1).add(new FuncOption("d2i", new Pair()));
					}
					tt1 = TokenType.INT;
				}
				else if(tt1 == TokenType.CHAR) {
					tt1 = TokenType.INT;
				}
			}
			else if(tt2 == TokenType.DOUBLE) {
				if(tt1 == TokenType.INT) {
					if(level == 0) {
						startTable.add(new Start("i2d", new Pair()));
					}
					else {
						funcOpTable.get(funcNum-1).add(new FuncOption("i2d", new Pair()));
					}
					tt1 = TokenType.DOUBLE;
				}
				else if(tt1 == TokenType.CHAR) {
					if(level == 0) {
						startTable.add(new Start("i2d", new Pair()));
					}
					else {
						funcOpTable.get(funcNum-1).add(new FuncOption("i2d", new Pair()));
					}
					tt1 = TokenType.DOUBLE;
				}
			}
		}
		return new Error(tt1);
	}
	
	// <一元表达式> -> [<一元操作符>] <主要表达式>
	private Error unaryExpre() {
		Token token = nextToken();
		if(token.getTokenType() == TokenType.PLUS_SIGN 
				|| token.getTokenType() == TokenType.MINUS_SIGN) {
			Error err = priExpre();
			if(err.isError()) return err;
			if(token.getTokenType() == TokenType.MINUS_SIGN) {
				if(level == 0) {
					if(err.getTokenType() == TokenType.INT || err.getTokenType() == TokenType.CHAR)
						startTable.add(new Start("ineg", new Pair()));
					else if(err.getTokenType() == TokenType.DOUBLE)
						startTable.add(new Start("dneg", new Pair()));
				}
				else {
					if(err.getTokenType() == TokenType.INT || err.getTokenType() == TokenType.CHAR)
						funcOpTable.get(funcNum-1).add(new FuncOption("ineg", new Pair()));
					else if(err.getTokenType() == TokenType.DOUBLE)
						funcOpTable.get(funcNum-1).add(new FuncOption("dneg", new Pair()));
					
				}
			}
			return new Error(err.getTokenType());
		}//代码生成的时候有用
		else {
			unreadToken();//回退
			Error err = priExpre();
			if(err.isError()) return err;
			return new Error(err.getTokenType());
		}
		
	}
	
	// <主要表达式> -> '(' <表达式> ')' | <标识符> | <数字> | <函数调用>
	//标识符和表达式的类型还没判断
	private Error priExpre()  {
		Token token = nextToken();
		
		switch(token.getTokenType()) {
			case LEFT_BRACKET:{
				Error err = expression();
				if(err.isError() == false) {
					//为空 证明这是正确的 我们继续判断右括号
					token = nextToken();
					if(token.getTokenType() == TokenType.RIGHT_BRACKET)
						return new Error(err.getTokenType());//正确 返回空
					else
						return new Error(token.getPos(), ErrorType.NO_RIGHT_BRACKET);
				}
				else {
					return err;
				}
			}
			case IDENTIFIER:{//根据标识符所属的符号表判断是标识符还是函数调用
				//先判断是否定义，直接在符号表里找
				//再判断属于哪里
				//如果是函数 要判断返回值是不是void 如果是标识符 要判断类型是不是void
				IdentiKind ik = isAlreadyDecAndNoVoid(token.getValue());
				if(ik == null)
					return new Error(token.getPos(), ErrorType.NO_DECLARED);
				else if(ik == IdentiKind.FUNCTION) {
					String name = token.getValue();
					IdentiType it = findFunc(name).getRetType();
					if(it == IdentiType.VOID)
						return new Error(token.getPos(), ErrorType.CANNOT_ASSIGN_VOID);
					unreadToken();
					Error err = funcCall(false);
					if(err.isError()) return err;
					if(it == IdentiType.CHAR) {
						return new Error(TokenType.CHAR);
					}
					else if(it == IdentiType.INT) {
						return new Error(TokenType.INT);
					}
					else if(it == IdentiType.DOUBLE) {
						return new Error(TokenType.DOUBLE);
					}
				}
				else {//变量和参数，获取地址，加载值
					//先判断是否是void
					IdentiType it = getIdentiType(token.getValue());
					Pair p1 = getLevelandIndex(token.getValue());
					if(level == 0) {
						if(it == IdentiType.INT) {
							startTable.add(new Start("loada", p1));
							startTable.add(new Start("iload", new Pair()));
							return new Error(TokenType.INT);
						}
						else if(it == IdentiType.CHAR) {
							startTable.add(new Start("loada", p1));
							startTable.add(new Start("iload", new Pair()));
							return new Error(TokenType.CHAR);
						}
						else if(it == IdentiType.DOUBLE) {
							startTable.add(new Start("loada", p1));
							startTable.add(new Start("dload", new Pair()));
							return new Error(TokenType.DOUBLE);
						}
					}
					else {
						if(it == IdentiType.INT) {
							funcOpTable.get(funcNum-1).add(new FuncOption("loada", p1));
							funcOpTable.get(funcNum-1).add(new FuncOption("iload", new Pair()));
							return new Error(TokenType.INT);
						}
						else if(it == IdentiType.CHAR) {
							funcOpTable.get(funcNum-1).add(new FuncOption("loada", p1));
							funcOpTable.get(funcNum-1).add(new FuncOption("iload", new Pair()));
							return new Error(TokenType.CHAR);
						}
						else if(it == IdentiType.DOUBLE) {
							funcOpTable.get(funcNum-1).add(new FuncOption("loada", p1));
							funcOpTable.get(funcNum-1).add(new FuncOption("dload", new Pair()));
							return new Error(TokenType.DOUBLE);
						}
						
					}
					if(it == IdentiType.CHAR) {
						return new Error(TokenType.CHAR);
					}
					else if(it == IdentiType.INT) {
						return new Error(TokenType.INT);
					}
					else if(it == IdentiType.DOUBLE) {
						return new Error(TokenType.DOUBLE);
					}
				}
				break;
			}
			case DEC_INTEGER:
			case HEX_INTEGER:{
				if(level == 0) {
					startTable.add(new Start("ipush", new Pair(Integer.valueOf(token.getValue()))));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("ipush", new Pair(Integer.valueOf(token.getValue()))));
				}
				return new Error(TokenType.INT);//告诉上边的我这是个int整数
			}
			case CHARACTER:{
				if(level == 0) {
					startTable.add(new Start("bipush", new Pair((int)token.getValue().charAt(0))));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("bipush", new Pair((int)token.getValue().charAt(0))));
				}
				return new Error(TokenType.CHAR);
			}
			case DOUBLE_DIGIT:{
				//在常量表里添加这个double
				constTable.add(new Constant(constIndex, "D", Double.parseDouble(token.getValue())));
				if(level == 0) {
					startTable.add(new Start("loadc", new Pair(constIndex)));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("loadc", new Pair(constIndex)));
				}
				constIndex++;
				return new Error(TokenType.DOUBLE);
			}
			default:{
				return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
			}
		}
		return new Error();
	}
	
	//<函数声明> -> <类型><标识符><参数><合成语句>
	@SuppressWarnings("unchecked")
	private Error funcDef() {
		//类型
		Token token = nextToken();
		retType = token.getTokenType();
		IdentiType it = null;
		if(token.getTokenType() != TokenType.VOID
				&& token.getTokenType() != TokenType.INT
				&& token.getTokenType() != TokenType.DOUBLE
				&& token.getTokenType() != TokenType.CHAR) {
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		}
		else if(token.getTokenType() == TokenType.VOID) {
			it = IdentiType.VOID;
		}
		else if(token.getTokenType() == TokenType.INT){
			it = IdentiType.INT;
		}
		else if(token.getTokenType() == TokenType.DOUBLE) {
			it = IdentiType.DOUBLE;
		}
		else if(token.getTokenType() == TokenType.CHAR) {
			it = IdentiType.CHAR;
		}
		//标识符 也即函数名
		token = nextToken();
		if(token.getTokenType() != TokenType.IDENTIFIER)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		String name = token.getValue();
		//函数进符号表
		if(isSecondName(token.getValue()))
			return new Error(token.getPos(), ErrorType.DUPLICATE_DECLARATION);
		symbolTable.push(new Symbol(token, it, IdentiKind.FUNCTION, level));
		
		//参数
		paraList.clear();
		Error err = paraClause();
		if(err.isError()) return err;
		//常量表
		constTable.add(new Constant(constIndex, "S", name));
		//函数表
		funcTable.add(new Functions(funcNum, constIndex, paraList.size()));
		constIndex++;
		ArrayList<Pair> paralist = new ArrayList<>();
		paralist = (ArrayList<Pair>)paraList.clone();
		indexTable.add(new IndexTable(name, it, paralist));
		funcOpTable.add(new ArrayList<FuncOption>());
		funcNum ++;
		err = compoundState();
		if(err.isError()) return err;
		//无脑加返回指令
		if(it == IdentiType.VOID) {
			funcOpTable.get(funcNum-1).add(new FuncOption("ret", new Pair()));
		}
		else {//返回一个0
			if(it == IdentiType.CHAR || it == IdentiType.INT) {
				funcOpTable.get(funcNum-1).add(new FuncOption("ipush", new Pair(0)));
				funcOpTable.get(funcNum-1).add(new FuncOption("iret", new Pair()));
			}
			else if(it == IdentiType.DOUBLE) {
				funcOpTable.get(funcNum-1).add(new FuncOption("snew", new Pair(2)));
				funcOpTable.get(funcNum-1).add(new FuncOption("dret", new Pair()));
			}
		}
		return new Error();
	}
	
	//<参数> -> '(' [<参数声明列表>] ')'
	private Error paraClause() {
		Token token = nextToken();
		if(token.getTokenType() != TokenType.LEFT_BRACKET)
			return new Error(token.getPos(), ErrorType.NO_LEFT_BRACKET);
		
		//参数属于下一个作用域
		level++;
	
		token = nextToken();
		unreadToken();//预读
		if(token.getTokenType() == TokenType.INT
				|| token.getTokenType() == TokenType.CONST
				|| token.getTokenType() == TokenType.DOUBLE
				|| token.getTokenType() == TokenType.CHAR) {
			Error err = paraDecList();
			if(err.isError()) return err;
		}
		
		token = nextToken();
		if(token.getTokenType() != TokenType.RIGHT_BRACKET)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		level--;
		
		return new Error();
	}
	
	//<参数声明列表> -> <参数声明>{ ',' <参数声明>}
	private Error paraDecList() {
		Error err = paraDec();
		if(err.isError()) return err;
		Token token = nextToken();
		while(token.getTokenType() == TokenType.COMMA) {
			err = paraDec();
			if(err.isError()) return err;
			token = nextToken();
		}
		unreadToken();
		return new Error();
	}
	
	//<参数声明> -> [<const>]<类型> <标识符>
	private Error paraDec() {
		boolean isConst = false;
		
		Token token = nextToken();
		if(token.getTokenType() == TokenType.CONST) {
			token = nextToken();
			isConst = true;
		}
			
		TokenType tt = token.getTokenType();
		if(tt != TokenType.INT
				&& tt != TokenType.DOUBLE
				&& tt != TokenType.CHAR)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		token = nextToken();
		if(token.getTokenType() != TokenType.IDENTIFIER)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		
		//参数进符号表,参数也要判断是否和本作用域重名,因为有可能参数里重名
		if(isSecondName(token.getValue()))
			return new Error(token.getPos(), ErrorType.DUPLICATE_DECLARATION);
		
		IdentiType it = null;
		//索引表需要记下参数
		if(isConst) {
			if(tt == TokenType.INT) {
				paraList.add(new Pair(token.getValue(), IdentiType.CONST_INT));
				it = IdentiType.CONST_INT;
			}
			else if(tt == TokenType.DOUBLE){
				paraList.add(new Pair(token.getValue(), IdentiType.CONST_DOUBLE));
				it = IdentiType.CONST_DOUBLE;
			}
			else if(tt == TokenType.CHAR) {
				paraList.add(new Pair(token.getValue(), IdentiType.CONST_CHAR));
				it = IdentiType.CONST_CHAR;
			}
		}
		else {
			if(tt == TokenType.INT) {
				paraList.add(new Pair(token.getValue(), IdentiType.INT));
				it = IdentiType.INT;
			}
			else if(tt == TokenType.DOUBLE){
				paraList.add(new Pair(token.getValue(), IdentiType.DOUBLE));
				it = IdentiType.DOUBLE;
			}
			else if(tt == TokenType.CHAR) {
				paraList.add(new Pair(token.getValue(), IdentiType.CHAR));
				it = IdentiType.CHAR;
			}
		}
		if(isConst) {
			symbolTable.push(new Symbol(token, it, IdentiKind.PARAMETER, level));
		}
		else
			symbolTable.push(new Symbol(token, it, IdentiKind.PARAMETER, level));
		
		return new Error();
	}
	
	//<函数调用> -> <标识符> '(' [<表达式列表>] ')'
	private Error funcCall(boolean isPop)  {
		Token token = nextToken();
		if(token.getTokenType() != TokenType.IDENTIFIER) {
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		}	
		String name = token.getValue();
		int index1 = isFuncBeenDec(name);
		if(index1 == -1) {
			return new Error(token.getPos(), ErrorType.NO_DECLARED);
		}
		
		////嘿嘿嘿,基础C0的参数只有int和const int，这两个不用区分啊嘿嘿嘿
		////所以目前只需要判断数量是否一致
		//强制类型转换
		paraNum = 0;//置零
		token = nextToken();
		if(token.getTokenType() != TokenType.LEFT_BRACKET)
			return new Error(token.getPos(), ErrorType.NO_LEFT_BRACKET);

		token = nextToken();
		if(token.getTokenType() != TokenType.RIGHT_BRACKET) {
			unreadToken();
			Error err = expreList(findFunc(name).getTokenType());
			if(err.isError()) return err;
			token = nextToken();
			if(token.getTokenType() != TokenType.RIGHT_BRACKET)
				return new Error(token.getPos(), ErrorType.NO_RIGHT_BRACKET);
		}
//		if(paraNum != findFunc(name).getParaNum()) {
//			return new Error(token.getPos(), ErrorType.PARAMETER_TYPE_ERROR);
//		}
		
		
		//如果这些都没错 那我就call
		funcOpTable.get(funcNum-1).add(new FuncOption("call", new Pair(index1)));
		//如果是空 本来就没有返回值 就不要pop了
//		if(isPop && indexTable.get(index1).getRetType() != IdentiType.VOID) {
//			funcOpTable.get(funcNum-1).add(new FuncOption("pop", new Pair()));
//		}
		return new Error();
	}
	
	//<表达式列表> -> <表达式> { ','<表达式> }
	private Error expreList(ArrayList<TokenType> paraList)  {
		int num = paraList.size();
//		ArrayList<TokenType> paraCallType = new ArrayList<>();
		
		for(int i = 0; i < num; i++) {
			Error err = expression();
			if(err.isError()) return err;
//			System.out.println(err.getTokenType());
//			System.out.println(paraList.get(i));
			transType(err.getTokenType(), paraList.get(i));
			Token token = nextToken();
			if(token.getTokenType() != TokenType.COMMA) {
				if(i != num-1) {
					return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
				}
				else {
					unreadToken();
				}
			}
		}
		return new Error();
	}
	
	//<合成语句> -> '{' {<变量声明>}<语句序列> '}'
	private Error compoundState()  {
		Token token = nextToken();
		if(token.getTokenType() != TokenType.LEFT_BRACE)
			return new Error(token.getPos(), ErrorType.NO_LEFT_BRACE);
		
		level++;
		
		token = nextToken();
		while(token.getTokenType() == TokenType.CONST
				|| token.getTokenType() == TokenType.INT
				|| token.getTokenType() == TokenType.DOUBLE
				|| token.getTokenType() == TokenType.CHAR) {
			unreadToken();
			Error err = varDec();
			if(err.isError()) return err;
			token = nextToken();
		}
		unreadToken();
		if(isStatementHead(token)) {
			Error err = stateSeq();
			if(err.isError()) return err;
		}
		token = nextToken();
		if(token.getTokenType() != TokenType.RIGHT_BRACE)
			return new Error(token.getPos(), ErrorType.NO_RIGHT_BRACE);
		
		//当前层级的全都出栈
		int popNum = 0;
		while(symbolTable.peek().getLevel() == level) {
//			symbolTable.peek().printSymbol();
			popNum ++;
			symbolTable.pop();
		}
//		if(!isFun) {
//			while(popNum >= 0) {
//				funcOpTable.get(funcNum-1).add(new FuncOption("pop", new Pair()));
//				popNum--;
//			}
//		}
		
		level--;
		
		return new Error();
	}
	
	//<语句序列> -> {<语句>}
	private Error stateSeq() {
		Token token = nextToken();
		while(isStatementHead(token)) {
			unreadToken();
			Error err = statement();
			if(err.isError()) return err;
			token = nextToken();
		}
		unreadToken();
		return new Error();
	}
	
	//<语句> -> '{' <语句序列> '}' | <条件语句> | <循环语句>
	// | <跳转语句> | <输出语句> | <输入语句> 
	// | <赋值表达式> ';' | <函数调用> ';' | ';' 
	private Error statement()  {
		Token token = nextToken();
		switch(token.getTokenType()) {
			case LEFT_BRACE:{
				level++;
				Error err = stateSeq();
				if(err.isError()) return err;
				token = nextToken();
				if(token.getTokenType() != TokenType.RIGHT_BRACE)
					return new Error(token.getPos(), ErrorType.NO_RIGHT_BRACE);
				
				//当前层级的全都出栈
				while(symbolTable.peek().getLevel() == level) {
//					symbolTable.peek().printSymbol();
					symbolTable.pop();
				}
				level--;
				break;
			}
			case IF:{
				unreadToken();
				Error err = conditionState();
				if(err.isError()) return err;
				break;
			}
			case WHILE:{
				unreadToken();
				Error err = loopState();
				if(err.isError()) return err;
				break;
			}
			case RETURN:{
				unreadToken();
				Error err = jumpState();
				if(err.isError()) return err;
				break;
			}
			case PRINT:{
				unreadToken();
				Error err = printState();
				if(err.isError()) return err;
				break;
			}
			case SCAN:{
				unreadToken();
				Error err = scanState();
				if(err.isError()) return err;
				break;
			}
			case IDENTIFIER:{
				//用符号表第一个找到的类型判断它到底是啥
				IdentiKind ik = isAlreadyDec(token.getValue());
				//未声明不管，交给赋值语句和函数调用自己处理
				if(ik == IdentiKind.FUNCTION) {
					unreadToken();
					Error err = funcCall(true);
					if(err.isError()) return err;
				}
				else {
					unreadToken();
					Error err = assignExpre();
					if(err.isError()) return err;
				}
				token = nextToken();
				if(token.getTokenType() != TokenType.SEMICOLON)
					return new Error(token.getPos(), ErrorType.NO_SEMICOLON_ERROR);
			}
			case SEMICOLON:{
				break;
			}
			default:{
				return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
			}
		}
		return new Error();
	}
	
	//<跳转语句> -> <返回语句>
	private Error jumpState()  {
		Error err = retState();
		if(err.isError()) return err;
		return new Error();
	}
	
	//<返回语句> -> 'return' [<表达式>] ';'
	private Error retState()  {
		Token token = nextToken();
		if(token.getTokenType() != TokenType.RETURN)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		token = nextToken();
		if(token.getTokenType() != TokenType.SEMICOLON) {
			if(indexTable.get(funcNum-1).getRetType() == IdentiType.VOID) {
				return new Error(token.getPos(), ErrorType.RUTURN_VALUE_TYPE_ERROR);
			}
			unreadToken();
			Error err = expression();
			if(err.isError()) return err;
			transType(err.getTokenType(), retType);
			if(retType == TokenType.INT || retType == TokenType.CHAR)
				funcOpTable.get(funcNum-1).add(new FuncOption("iret", new Pair()));
			else if(retType == TokenType.DOUBLE)
				funcOpTable.get(funcNum-1).add(new FuncOption("dret", new Pair()));
				
			token = nextToken();
			if(token.getTokenType() != TokenType.SEMICOLON)
				return new Error(token.getPos(), ErrorType.NO_SEMICOLON_ERROR);
		}else {
			if(indexTable.get(funcNum-1).getRetType() == IdentiType.VOID) {
				funcOpTable.get(funcNum-1).add(new FuncOption("ret", new Pair()));
			}
			else {
				return new Error(token.getPos(), ErrorType.RUTURN_VALUE_TYPE_ERROR);
			}
		}
		return new Error();
	}
	
	//<条件语句> -> 'if' '(' <状态> ')'<语句> ['else'<语句>]
	private Error conditionState()  {
		Token token = nextToken();
		if(token.getTokenType() != TokenType.IF)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		token = nextToken();
		if(token.getTokenType() != TokenType.LEFT_BRACKET)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		Error err = condition();
		if(err.isError()) return err;
		
		token = nextToken();
		if(token.getTokenType() != TokenType.RIGHT_BRACKET)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		err = statement();
//		token = nextToken();
//		if(token.getTokenType() == TokenType.LEFT_BRACE) {
//			unreadToken();
//			err = compoundState();
//		}
//		else {
//			unreadToken();
//			err = statement();
//		}
			
		
		if(err.isError()) return err;
		
		token = nextToken();
		if(token.getTokenType() != TokenType.ELSE) {
			//我需要在当前函数的table里找到被我暂存的条件跳转语句
			//然后修改它
			setJmpInstruct();
			unreadToken();
		}
		else {
			funcOpTable.get(funcNum-1).add(new FuncOption("jmp", new Pair()));//if如果跑了，就不跑else了
			setJmpInstruct();
			err = statement();
//			token = nextToken();
//			if(token.getTokenType() == TokenType.LEFT_BRACE) {
//				unreadToken();
//				err = compoundState();
//			}
//			else {
//				unreadToken();
//				err = statement();
//			}
			if(err.isError()) return err;
			setJmpInstruct();
		}
		
		return new Error();
	}
	
	//<循环语句> -> 'while' '('<状态>')'<合成语句>
	private Error loopState()  {
		Token token = nextToken();
		if(token.getTokenType() != TokenType.WHILE)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		token = nextToken();
		if(token.getTokenType() != TokenType.LEFT_BRACKET)
			return new Error(token.getPos(), ErrorType.NO_LEFT_BRACKET);
		
		//这将会成为condition里第一句指令的下标
		int index1 = funcOpTable.get (funcNum-1).size();
		Error err = condition();
		if(err.isError()) return err;
		
		token = nextToken();
		if(token.getTokenType() != TokenType.RIGHT_BRACKET)
			return new Error(token.getPos(), ErrorType.NO_RIGHT_BRACKET);
		
		err = statement();
//		token = nextToken();
//		if(token.getTokenType() == TokenType.LEFT_BRACE) {
//			unreadToken();
//			err = compoundState();
//		}
//		else {
//			unreadToken();
//			err = statement();
//		}
		if(err.isError()) return err;
		funcOpTable.get(funcNum-1).add(new FuncOption("jmp", new Pair(index1)));
		setJmpInstruct();
		return new Error();
	}
	
	//<输入语句> -> 'scan' '('<标识符>')' ';'
	private Error scanState() {
		Token token = nextToken();
		if(token.getTokenType() != TokenType.SCAN)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		token = nextToken();
		if(token.getTokenType() != TokenType.LEFT_BRACKET)
			return new Error(token.getPos(), ErrorType.NO_LEFT_BRACKET);
		
		token = nextToken();
		if(token.getTokenType() != TokenType.IDENTIFIER)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		//scan的标识符必须是非const值，而且这个值必须存在，且不是函数
		if(isAbleToAssign(token.getValue())) {
			Pair p1 = getLevelandIndex(token.getValue());
			funcOpTable.get(funcNum-1).add(new FuncOption("loada", p1));
			funcOpTable.get(funcNum-1).add(new FuncOption("iscan", new Pair()));
			funcOpTable.get(funcNum-1).add(new FuncOption("istore", new Pair()));
		}else {
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		}
		
		token = nextToken();
		if(token.getTokenType() != TokenType.RIGHT_BRACKET)
			return new Error(token.getPos(), ErrorType.NO_RIGHT_BRACKET);
		
		token = nextToken();
		if(token.getTokenType() != TokenType.SEMICOLON)
			return new Error(token.getPos(), ErrorType.NO_SEMICOLON_ERROR);
		return new Error();
	}
	
	//<输出语句> -> 'print' '('[<输出列表>]')'';'
	private Error printState()  {
		Token token = nextToken();
		if(token.getTokenType() != TokenType.PRINT)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		token = nextToken();
		if(token.getTokenType() != TokenType.LEFT_BRACKET)
			return new Error(token.getPos(), ErrorType.NO_LEFT_BRACKET);
		
		token = nextToken();
		if(token.getTokenType() != TokenType.RIGHT_BRACKET) {
			unreadToken();
			Error err = printList();
			if(err.isError()) return err;
			token = nextToken();
			if(token.getTokenType() != TokenType.RIGHT_BRACKET)
				return new Error(token.getPos(), ErrorType.NO_RIGHT_BRACKET);
		}
		
		token = nextToken();
		if(token.getTokenType() != TokenType.SEMICOLON)
			return new Error(token.getPos(), ErrorType.NO_SEMICOLON_ERROR);
		//输出换行
		funcOpTable.get(funcNum-1).add(new FuncOption("printl", new Pair()));
		
		return new Error();
	}
	
	//<输出列表> -> <输出> { ',' <输出> }
	private Error printList()  {
		Error err = print();
		if(err.isError()) return err;
		Token token = nextToken();
		while(token.getTokenType() == TokenType.COMMA) {
			funcOpTable.get(funcNum-1).add(new FuncOption("bipush", new Pair(32)));
			funcOpTable.get(funcNum-1).add(new FuncOption("cprint", new Pair()));
			err = print();
			if(err.isError()) return err;
			token = nextToken();
		}
		unreadToken();
		return new Error();
	}
	
	//<输出> -> <表达式> | <字符> | <字符串>
	private Error print()  {
		Token token = nextToken();//预读
		if(token.getTokenType() == TokenType.CHARACTER) {
//			int a = Integer.parseInt(token.getValue());
			char a;
//			System.out.println(token.getValue());
//			if(token.getValue().length() == 1) {
				a = token.getValue().charAt(0);
//			}
			
			funcOpTable.get(funcNum-1).add(new FuncOption("bipush", new Pair((int)a)));
			funcOpTable.get(funcNum-1).add(new FuncOption("cprint", new Pair()));
		}
		else if(token.getTokenType() == TokenType.STRING) {
			//常量表
			constTable.add(new Constant(constIndex, "S", token.getValue()));
			funcOpTable.get(funcNum-1).add(new FuncOption("loadc", new Pair(constIndex)));
			constIndex++;
			funcOpTable.get(funcNum-1).add(new FuncOption("sprint", new Pair()));
		}
		else {
			unreadToken();
			Error err = expression();
			if(err.isError()) return err;
			funcOpTable.get(funcNum-1).add(new FuncOption("iprint", new Pair()));
		}
		
		return new Error();
	}
	
	private Token nextToken() {
		return tokenList.get(index++);
	}
	
	private void unreadToken() {
		index--;
	}
	
	private Token getLastToken() {
		return tokenList.get(index-1);
	}
	
	private boolean isCompareSign(Token token) {
		if(token.getTokenType() == TokenType.LESS_SIGN
				|| token.getTokenType() == TokenType.LESS_OR_EQUAL_SIGN
				|| token.getTokenType() == TokenType.DOUBLE_EQUAL_SIGN
				|| token.getTokenType() == TokenType.NOT_EQUAL_SIGN
				|| token.getTokenType() == TokenType.GREATER_SIGN
				|| token.getTokenType() == TokenType.GREATER_OR_EQUAL_SIGN) {
			return true;
		}
		else
			return false;
	}
	
	private boolean isStatementHead(Token token) {
		if(token.getTokenType() == TokenType.LEFT_BRACE
				|| token.getTokenType() == TokenType.IF
				|| token.getTokenType() == TokenType.WHILE
				|| token.getTokenType() == TokenType.IDENTIFIER
				|| token.getTokenType() == TokenType.SCAN
				|| token.getTokenType() == TokenType.PRINT
				|| token.getTokenType() == TokenType.SEMICOLON
				|| token.getTokenType() == TokenType.RETURN)
			return true;
		
		return false;
	}
	
	//判断标识符名称在本层级中是否重复
	private boolean isSecondName(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		boolean flag = false;
		while(symbolTable.size() != 0 && symbolTable.peek().getLevel() == level) {
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name)) {
				flag = true;//重复了
				 break;
			}
		}
		while(tmp.size() != 0) {
			symbolTable.push(tmp.pop());//倒回来
		}
		return flag;
	}
	
	//判断是否可赋值 也即 是否已声明 是否是变量/参数
	private boolean isAbleToAssign1(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		boolean flag = false;
		while(symbolTable.size() != 0) {//从里往外找到的第一个同名的就是
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name) 
					&& (sb.getKind() == IdentiKind.VARIABLE || sb.getKind() == IdentiKind.PARAMETER)) {
				flag = true;//可合法赋值
				break;
			}
		}
		while(tmp.size() != 0) {
			symbolTable.push(tmp.pop());//倒回来
		}
		return flag;
	}
	
	//前提是已经经过1的判断了
	private TokenType isAbleToAssign2(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		
		TokenType tt = null;
		while(symbolTable.size() != 0) {
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name)) {
				//可合法赋值
				if(sb.getType() == IdentiType.CHAR) {
					tt = TokenType.CHAR;
				}
				else if(sb.getType() == IdentiType.INT) {
					tt = TokenType.INT;
				}
				else if(sb.getType() == IdentiType.DOUBLE) {
					tt = TokenType.DOUBLE;
				}
				break;
			}
		}
		while(tmp.size() != 0) {
			symbolTable.push(tmp.pop());//倒回来
		}
		return tt;
	}
	
	//判断这个单词是否存在 若存在 是否是变量或者可变参数 给scan用
	private boolean isAbleToAssign(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		boolean flag = false;
		while(symbolTable.size() != 0) {
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name) 
					&& (sb.getKind() == IdentiKind.PARAMETER || sb.getKind() == IdentiKind.VARIABLE)
					&& (sb.getType() == IdentiType.INT || sb.getType() == IdentiType.CHAR
					|| sb.getType() == IdentiType.DOUBLE)) {
				flag = true;//可合法赋值
				break;
			}
		}
		while(tmp.size() != 0) {
			symbolTable.push(tmp.pop());//倒回来
		}
		return flag;
	}
	
	//判断函数是否已经声明
	//找到这个函数在函数表里的下标
	//未声明就返回-1
	private int isFuncBeenDec(String name) {
		for(int i = 0; i < funcNum; i++) {
			if(name.equals(indexTable.get(i).getName())) {
				return i;
			}
		}
		return -1;
	}
	
	
	//判断该标识符的类型，包括函数、参数、变量
	//如果标识符未声明，返回null
	private IdentiKind isAlreadyDec(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		IdentiKind ik = null;
		while(symbolTable.size() != 0) {//从里往外找到的第一个同名的就是
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name)) {
				if(sb.getKind() == IdentiKind.VARIABLE) {
					ik = IdentiKind.VARIABLE;
				}
				else if(sb.getKind() == IdentiKind.PARAMETER) {
					ik = IdentiKind.PARAMETER;
				}
				else if(sb.getKind() == IdentiKind.FUNCTION) {
					ik = IdentiKind.FUNCTION;
				}	
				break;
			}
		}
		while(tmp.size() != 0) {
			symbolTable.push(tmp.pop());//倒回来
		}
		return ik;//ik==null表示没找到
	}
	
	
	private IdentiKind isAlreadyDecAndNoVoid(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		IdentiKind ik = null;
		while(symbolTable.size() != 0) {//从里往外找到的第一个同名的就是
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name)) {
				if(sb.getType() == IdentiType.CONST_VOID || sb.getType() == IdentiType.VOID) {
					//虽然找到了 但是类型是void 没用
					break;
				}
				if(sb.getKind() == IdentiKind.VARIABLE) {
					ik = IdentiKind.VARIABLE;
				}
				else if(sb.getKind() == IdentiKind.PARAMETER) {
					ik = IdentiKind.PARAMETER;
				}
				else if(sb.getKind() == IdentiKind.FUNCTION) {
					ik = IdentiKind.FUNCTION;
				}	
				break;
			}
		}
		while(tmp.size() != 0) {
			symbolTable.push(tmp.pop());//倒回来
		}
		return ik;//ik==null表示没找到
	}
	
	//判断是否是int，double或者char
	private boolean isOkType(TokenType type) {
		if(type == TokenType.INT || type == TokenType.DOUBLE || type == TokenType.CHAR) {
			return true;
		}
		else {
			return false;
		}
	}
	
	//是否是变量的开始
	//变量开始const ... | (int | double | char) identifier
	private boolean isVariableHead() {
		Token token = nextToken();
		if(token.getTokenType() == TokenType.CONST) {
			unreadToken();
			return true;
		}
		else if(isOkType(token.getTokenType())) {
			token = nextToken();
			if(token.getTokenType() == TokenType.IDENTIFIER) {
				token = nextToken();
				if(token.getTokenType() != TokenType.LEFT_BRACKET) {
					//不等于左括号的咱们一律当做变量处理，有错的话后面会发现
					unreadToken();
					unreadToken();
					unreadToken();
					return true;
				}
				unreadToken();
			}
			unreadToken();
		}
		unreadToken();
		return false;
	}
	
	//是否是函数的开始
	//函数开始void|int|double|char
	private boolean isFuncHead() {
		Token token = nextToken();
		if(token.getTokenType() == TokenType.VOID) {
			unreadToken();
			return true;
		}
		else if(isOkType(token.getTokenType())) {
			token = nextToken();
			if(token.getTokenType() == TokenType.IDENTIFIER) {
				token = nextToken();
				if(token.getTokenType() == TokenType.LEFT_BRACKET) {
					unreadToken();
					unreadToken();
					unreadToken();
					return true;
				}
				unreadToken();
			}
			unreadToken();
		}
		unreadToken();
		return false;
	}
	
	//判断是否有main函数
	private boolean isHaveMain() {
		int size = indexTable.size();
		for(int i = 0; i < size; i++) {
//			System.out.println(indexTable.get(i).getName());
			if(indexTable.get(i).getName().equals("main")) {
				return true;
			}
		}
		return false;
	}
	
	//找到这个函数在索引表中的对象
	private IndexTable findFunc(String name) {
		int size = indexTable.size();
		for(int i = 0; i < size; i++) {
			if(indexTable.get(i).getName().equals(name)) {
				return indexTable.get(i);
			}
		}
		return null;
	}
	
	//通过标识符的名称 找标识符的函数嵌套的层级差和下标
	//在符号表里找
	//之前已经判断过了，这个符号一定会存在
	private Pair getLevelandIndex(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		int index1 = 0;
		int level1 = 0;
		while(symbolTable.size() != 0) {//从里往外找到的第一个同名的就是
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name)) {
				//获得这个标识符的层级
				int level2 = sb.getLevel();
				if(level2 == 1) {
					level1 = 0;
					while(symbolTable.size() != 0) {
						Symbol sb1 = symbolTable.peek();
						if(sb1.getLevel() == 1) {
							if(sb1.getType() == IdentiType.DOUBLE ||
									sb1.getType() == IdentiType.CONST_DOUBLE)
								index1 += 2;
							else
								index1 ++;
							symbolTable.pop();
							tmp.push(sb1);
						}
						else {
							break;
						}
					}
				}
				else if(level2 == 0) {
					if(this.level == 0)
						level1 = 0;
					else
						level1 = 1;
					while(symbolTable.size() != 0) {
						Symbol sb1 = symbolTable.pop();
						tmp.push(sb1);
						if(sb1.getType() == IdentiType.DOUBLE ||
								sb1.getType() == IdentiType.CONST_DOUBLE)
							index1 += 2;
						else
							index1 ++;
					}
				}
				break;
			}
		}
		while(tmp.size() != 0) {
			symbolTable.push(tmp.pop());//倒回来
		}
		return new Pair(level1, index1);
	}
	
	//添加跳转指令
	private void addCompareInstruct(String s) {
		String jmp1 = new String();
		String jmp2 = new String("jmp");
		int a1 = funcOpTable.get(funcNum-1).size() + 2;
		if(s.equals("<")) {
			jmp1 = "jl";
		}
		else if(s.equals("<=")){
			jmp1 = "jle";
		}
		else if(s.equals(">")) {
			jmp1 = "jg";
		}
		else if(s.equals(">=")) {
			jmp1 = "jge";
		}
		else if(s.equals("==")) {
			jmp1 = "je";
		}
		else if(s.equals("!=")) {
			jmp1 = "jne";
		}
		funcOpTable.get(funcNum-1).add(new FuncOption(jmp1, new Pair(a1)));
		funcOpTable.get(funcNum-1).add(new FuncOption(jmp2, new Pair()));
	}
	
	//为之前留空的跳转指令设置offset
	private void setJmpInstruct() {
		ArrayList<FuncOption> tmp = funcOpTable.get(funcNum-1);
		int size = funcOpTable.get(funcNum-1).size();
		for(int i = size-2; i >= 0; i--) {
			if(isCompareInstruct(tmp.get(i).getOpcode())) {
				//找到了第一个跳转
				//而且没有offset
				if(tmp.get(i).getOperandNum() == 0) {
					funcOpTable.get(funcNum-1).get(i).setOperands(new Pair(size));
					break;
				}
			}
		}
	}
	
	//判断是否是跳转指令
	private boolean isCompareInstruct(String s) {
		if(s.equals("jl") || s.equals("jle")
				|| s.equals("jg") || s.equals("jge")
				|| s.equals("je") || s.equals("jne")
				|| s.equals("jmp")) {
			return true;
		}
		else {
			return false;
		}
	}
	
	//肯定存在，但是我想知道它的类型
	private IdentiType getIdentiType(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		IdentiType it = null;
		while(symbolTable.size() != 0) {//从里往外找到的第一个同名的就是
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name)) {
				it = sb.getType();
				break;
			}
		}
		while(tmp.size() != 0) {
			symbolTable.push(tmp.pop());//倒回来
		}
		return it;
	}
	
	//tt2是目标
	private TokenType transType(TokenType tt1, TokenType tt2) {
		if(tt2 == TokenType.CHAR) {
			if(tt1 == TokenType.DOUBLE) {
				if(level == 0) {
					startTable.add(new Start("d2i", new Pair()));
					startTable.add(new Start("i2c", new Pair()));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("d2i", new Pair()));
					funcOpTable.get(funcNum-1).add(new FuncOption("i2c", new Pair()));
				}
				tt1 = TokenType.CHAR;
			}
			else if(tt1 == TokenType.INT) {
				if(level == 0) {
					startTable.add(new Start("i2c", new Pair()));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("i2c", new Pair()));
				}
				tt1 = TokenType.CHAR;
			}
		}
		else if(tt2 == TokenType.INT) {
			if(tt1 == TokenType.DOUBLE) {
				if(level == 0) {
					startTable.add(new Start("d2i", new Pair()));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("d2i", new Pair()));
				}
				tt1 = TokenType.INT;
			}
			else if(tt1 == TokenType.CHAR) {
				tt1 = TokenType.INT;
			}
		}
		else if(tt2 == TokenType.DOUBLE) {
			if(tt1 == TokenType.INT) {
				if(level == 0) {
					startTable.add(new Start("i2d", new Pair()));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("i2d", new Pair()));
				}
				tt1 = TokenType.DOUBLE;
			}
			else if(tt1 == TokenType.CHAR) {
				if(level == 0) {
					startTable.add(new Start("i2d", new Pair()));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("i2d", new Pair()));
				}
				tt1 = TokenType.DOUBLE;
			}
		}
		return tt1;
	}
}



