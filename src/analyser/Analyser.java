package analyser;

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
//	private Vector<FuncOption> nowFuncTable = new Vector<>();
//	private int funcIndex = 0;
	
	File file;
	FileOutputStream fileOutputStream;
	PrintStream printStream;
	
	
	public void runAnalyser(String in, String out) throws IOException  {
		tokenList = tz.getTokenList(in);
		if(tokenList == null) //说明词法分析出错了
			return ;
		
		//控制台输出改到文件中
		file = new File(out);
		fileOutputStream = new FileOutputStream(file);
		printStream = new PrintStream(fileOutputStream);
		System.setOut(printStream);
		
		Error err = Program();
		if(err != null)
			err.printError();
		if(!isHaveMain())
			System.out.println("Error! There is not main function!");
		System.out.println(".constants");
		for(int i = 0; i < constTable.size(); i++) {
			constTable.get(i).print();
		}
		System.out.println(".start");
		for(int i = 0; i < startTable.size(); i++) {
			System.out.print(i + " ");
			startTable.get(i).print();
		}
		System.out.println(".functions");
		for(int i = 0; i < funcTable.size(); i++) {
			funcTable.get(i).print();
		}
//		System.out.println(funcOpTable.get(0));
		for(int i = 0; i < funcNum; i++) {
			System.out.println(".F" + i);
			for(int j = 0; j < funcOpTable.get(i).size(); j++) {
				System.out.print(j + " ");
				funcOpTable.get(i).get(j).print();
			}
		}
	}
	
	// <程序> -> {变量声明}{函数定义}
	// 返回空表示没有错误
	private Error Program() {
		while(isVariableHead()) {
			Error err = varDec();
			if(err != null) return err;
		}
		while(isFuncHead()) {
			Error err = funcDef();
			if(err != null) return err;
		}
		return null;
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
		
		if(token.getTokenType() != TokenType.INT && token.getTokenType() != TokenType.CHAR) {
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		}
		TokenType tt = token.getTokenType();
		
		Error err = initDecList(isConst, tt);
		if(err != null) return err;
		
		token = nextToken();
		if(token.getTokenType() != TokenType.SEMICOLON)
			return new Error(token.getPos(), ErrorType.NO_SEMICOLON_ERROR);
		return null;
	}
	
	
	//<初始化说明列表> -> <初始化说明>{','<初始化说明>}
	// a, b, c = 1
	private Error initDecList(boolean isConst, TokenType tt)  {
		Error err = initDec(isConst, tt);
		if(err != null) return err;
		Token token = nextToken();
		while(token.getTokenType() == TokenType.COMMA) {
			err = initDec(isConst, tt);
			if(err != null) return err;
			token = nextToken();
		}
		unreadToken();
		return null;
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
				symbolTable.push(new Symbol(token, IdentiType.CONST_INT, IdentiKind.VARIABLE, level));
			}
			else {
				symbolTable.push(new Symbol(token, IdentiType.INT, IdentiKind.VARIABLE, level));
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
				if(err != null) return err;
			}
			else {
				//赋初值0
				if(level == 0) {
					startTable.add(new Start("ipush", new Pair(0)));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("ipush", new Pair(0)));
				}
				
				unreadToken();
			}
		}
		else {
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		}
		return null;
	}
	
	//<初始化> -> ‘=’ <表达式>
	// = 1+9
	private Error init(TokenType tt)  {
		Token token = nextToken();
		if(token.getTokenType() == TokenType.EQUAL_SIGN) {
			Error err = expression();
			if(err != null) return err;
		}
		else
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		return null;
	}
	
	// <赋值表达式> -> <标识符>[ '=' <表达式>]
	private Error assignExpre()  {
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
			if(!isAbleToAssign2(name)) {
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
			if(err != null) return err;
			if(level == 0) {
				startTable.add(new Start("istore", new Pair()));
			}
			else {
				funcOpTable.get(funcNum-1).add(new FuncOption("istore", new Pair()));
			}
		}
		else
			unreadToken();
		
		return null;
	}
	
	// <状态> -> <表达式>[<比较符号> <表达式>]
	private Error condition()  {
		Error err = expression();
		if(err != null) return err;
		Token token = nextToken();
		if(isCompareSign(token)) {
			String compare = token.getValue();
			err = expression();
			if(err != null) return err;
			funcOpTable.get(funcNum-1).add(new FuncOption("isub", new Pair()));
			addCompareInstruct(compare);
		}
		else {
			int a1 = funcOpTable.get(funcNum-1).size() + 2;
			funcOpTable.get(funcNum-1).add(new FuncOption("jne", new Pair(a1)));
			funcOpTable.get(funcNum-1).add(new FuncOption("je", new Pair()));
			unreadToken();
		}
		return null;
	}
	
	// <表达式> -> <表达式语句>
	private Error expression()  {
		Error err = addExpre();
		if(err != null) return err;
		return null;
	}
	
	// <表达式语句> -> <项> { <加减符号> <项>}
	private Error addExpre()  {
		Error err = mulExpre();
		if(err != null) return err;
		Token token = nextToken();
		while(token.getTokenType() == TokenType.PLUS_SIGN 
				|| token.getTokenType() == TokenType.MINUS_SIGN) {
			err = mulExpre();
			if(err != null) return err;
			if(token.getTokenType() == TokenType.PLUS_SIGN) {
				if(level == 0) {
					startTable.add(new Start("iadd", new Pair()));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("iadd", new Pair()));
				}
			}
			else {
				if(level == 0) {
					startTable.add(new Start("isub", new Pair()));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("isub", new Pair()));
				}
			}
			token = nextToken();
		}
		unreadToken();
		return null;
	}
	
	// <项> -> <一元表达式> { <乘除符号> <一元表达式>}
	private Error mulExpre()  {
		Error err = unaryExpre();
		if(err != null) return err;
		Token token = nextToken();
		while(token.getTokenType() == TokenType.MUL_SIGN
				|| token.getTokenType() == TokenType.DIV_SIGN) {
			err = unaryExpre();
			if(err != null) return err;
			if(token.getTokenType() == TokenType.MUL_SIGN) {
				if(level == 0) {
					startTable.add(new Start("imul", new Pair()));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("imul", new Pair()));
				}
			}
			else {
				if(level == 0) {
					startTable.add(new Start("idiv", new Pair()));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("idiv", new Pair()));
				}
			}
			token = nextToken();
		}
		unreadToken();//之前多读了一个不是＋和-的，回退
		return null;
	}
	
	// <一元表达式> -> [<一元操作符>] <主要表达式>
	private Error unaryExpre() {
		Token token = nextToken();
		if(token.getTokenType() == TokenType.PLUS_SIGN 
				|| token.getTokenType() == TokenType.MINUS_SIGN) {
			Error err = priExpre();
			if(err != null) return err;
			if(token.getTokenType() == TokenType.MINUS_SIGN) {
				if(level == 0) {
					startTable.add(new Start("ineg", new Pair()));
				}
				else {
					funcOpTable.get(funcNum-1).add(new FuncOption("ineg", new Pair()));
				}
			}
		}//代码生成的时候有用
		else {
			unreadToken();//回退
			Error err = priExpre();
			if(err != null) return err;
		}
		return null;
	}
	
	// <主要表达式> -> '(' <表达式> ')' | <标识符> | <数字> | <函数调用>
	private Error priExpre()  {
		Token token = nextToken();
		
		switch(token.getTokenType()) {
			case LEFT_BRACKET:{
				Error err = expression();
				if(err == null) {
					//为空 证明这是正确的 我们继续判断右括号
					token = nextToken();
					if(token.getTokenType() == TokenType.RIGHT_BRACKET)
						return null;//正确 返回空
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
					if(findFunc(name).getRetType() == IdentiType.VOID)
						return new Error(token.getPos(), ErrorType.CANNOT_ASSIGN_VOID);
					unreadToken();
					Error err = funcCall(false);
					if(err != null) return err;
				}
				else {//变量和参数，获取地址，加载值
					//先判断是否是void
					Pair p1 = getLevelandIndex(token.getValue());
					if(level == 0) {
						startTable.add(new Start("loada", p1));
						startTable.add(new Start("iload", new Pair()));
					}
					else {
						funcOpTable.get(funcNum-1).add(new FuncOption("loada", p1));
						funcOpTable.get(funcNum-1).add(new FuncOption("iload", new Pair()));
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
				break;
			}
			default:{
				return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
			}
		}
		return null;
	}
	
	//<函数声明> -> <类型><标识符><参数><合成语句>
	@SuppressWarnings("unchecked")
	private Error funcDef() {
		//类型
		Token token = nextToken();
		IdentiType it;
		if(token.getTokenType() != TokenType.VOID
				&& token.getTokenType() != TokenType.INT) {
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		}
		else if(token.getTokenType() == TokenType.VOID) {
			it = IdentiType.VOID;
		}
		else {
			it = IdentiType.INT;
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
		if(err != null) return err;
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
		if(err != null) return err;
		//无脑加返回指令
		if(it == IdentiType.VOID) {
			funcOpTable.get(funcNum-1).add(new FuncOption("ret", new Pair()));
		}
		else {//返回一个0
			funcOpTable.get(funcNum-1).add(new FuncOption("ipush", new Pair(0)));
			funcOpTable.get(funcNum-1).add(new FuncOption("iret", new Pair()));
		}
		return null;
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
				|| token.getTokenType() == TokenType.CONST) {
			Error err = paraDecList();
			if(err != null) return err;
		}
		
		token = nextToken();
		if(token.getTokenType() != TokenType.RIGHT_BRACKET)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		level--;
		
		return null;
	}
	
	//<参数声明列表> -> <参数声明>{ ',' <参数声明>}
	private Error paraDecList() {
		Error err = paraDec();
		if(err != null) return err;
		Token token = nextToken();
		while(token.getTokenType() == TokenType.COMMA) {
			err = paraDec();
			if(err != null) return err;
			token = nextToken();
		}
		unreadToken();
		return null;
	}
	
	//<参数声明> -> [<const>]<类型> <标识符>
	private Error paraDec() {
		boolean isConst = false;
		
		Token token = nextToken();
		if(token.getTokenType() == TokenType.CONST) {
			token = nextToken();
			isConst = true;
		}
			
		if(token.getTokenType() != TokenType.INT)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		token = nextToken();
		if(token.getTokenType() != TokenType.IDENTIFIER)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		
		//参数进符号表,参数也要判断是否和本作用域重名,因为有可能参数里重名
		if(isSecondName(token.getValue()))
			return new Error(token.getPos(), ErrorType.DUPLICATE_DECLARATION);
		//索引表需要记下参数
		if(isConst) {
			paraList.add(new Pair(token.getValue(), IdentiType.CONST_INT));
		}
		else {
			paraList.add(new Pair(token.getValue(), IdentiType.INT));
		}
		if(isConst)
			symbolTable.push(new Symbol(token, IdentiType.CONST_INT, IdentiKind.PARAMETER, level));
		else
			symbolTable.push(new Symbol(token, IdentiType.INT, IdentiKind.PARAMETER, level));
		
		return null;
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
		
		//这里还没有判断参数是否相同 我脑子太乱了 以后再写 开动！
		//嘿嘿嘿,基础C0的参数只有int和const int，这两个不用区分啊嘿嘿嘿
		//所以目前只需要判断数量是否一致
		paraNum = 0;//置零
		token = nextToken();
		if(token.getTokenType() != TokenType.LEFT_BRACKET)
			return new Error(token.getPos(), ErrorType.NO_LEFT_BRACKET);

		token = nextToken();
		if(token.getTokenType() != TokenType.RIGHT_BRACKET) {
			unreadToken();
			Error err = expreList();
			if(err != null) return err;
			token = nextToken();
			if(token.getTokenType() != TokenType.RIGHT_BRACKET)
				return new Error(token.getPos(), ErrorType.NO_RIGHT_BRACKET);
		}
		if(paraNum != findFunc(name).getParaNum()) {
//			System.out.println(paraNum);
//			System.out.println(findFunc(name).getParaNum());
//			System.out.println(name);
			return new Error(token.getPos(), ErrorType.PARAMETER_TYPE_ERROR);
		}
		
		//如果这些都没错 那我就call
		funcOpTable.get(funcNum-1).add(new FuncOption("call", new Pair(index1)));
		//如果是空 本来就没有返回值 就不要pop了
		if(isPop && indexTable.get(index1).getRetType() != IdentiType.VOID) {
			funcOpTable.get(funcNum-1).add(new FuncOption("pop", new Pair()));
		}
		return null;
	}
	
	//<表达式列表> -> <表达式> { ','<表达式> }
	private Error expreList()  {
		Error err = expression();
		if(err != null) return err;
		paraNum++;
		Token token = nextToken();
		while(token.getTokenType() == TokenType.COMMA) {
			err = expression();
			if(err != null) return err;
			paraNum++;
			token = nextToken();
		}
		unreadToken();
		return null;
	}
	
	//<合成语句> -> '{' {<变量声明>}<语句序列> '}'
	private Error compoundState()  {
		Token token = nextToken();
		if(token.getTokenType() != TokenType.LEFT_BRACE)
			return new Error(token.getPos(), ErrorType.NO_LEFT_BRACE);
		
		level++;
		
		token = nextToken();
		while(token.getTokenType() == TokenType.CONST
				|| token.getTokenType() == TokenType.INT) {
			unreadToken();
			Error err = varDec();
			if(err != null) return err;
			token = nextToken();
		}
		unreadToken();
		if(isStatementHead(token)) {
			Error err = stateSeq();
			if(err != null) return err;
		}
		token = nextToken();
		if(token.getTokenType() != TokenType.RIGHT_BRACE)
			return new Error(token.getPos(), ErrorType.NO_RIGHT_BRACE);
		
		//当前层级的全都出栈
		while(symbolTable.peek().getLevel() == level) {
//			symbolTable.peek().printSymbol();
			symbolTable.pop();
		}
		level--;
		
		return null;
	}
	
	//<语句序列> -> {<语句>}
	private Error stateSeq()  {
		Token token = nextToken();
		while(isStatementHead(token)) {
			unreadToken();
			Error err = statement();
			if(err != null) return err;
			token = nextToken();
		}
		unreadToken();
		return null;
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
				if(err != null) return err;
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
				if(err != null) return err;
				break;
			}
			case WHILE:{
				unreadToken();
				Error err = loopState();
				if(err != null) return err;
				break;
			}
			case RETURN:{
				unreadToken();
				Error err = jumpState();
				if(err != null) return err;
				break;
			}
			case PRINT:{
				unreadToken();
				Error err = printState();
				if(err != null) return err;
				break;
			}
			case SCAN:{
				unreadToken();
				Error err = scanState();
				if(err != null) return err;
				break;
			}
			case IDENTIFIER:{
				//用符号表第一个找到的类型判断它到底是啥
				IdentiKind ik = isAlreadyDec(token.getValue());
				//未声明不管，交给赋值语句和函数调用自己处理
				if(ik == IdentiKind.FUNCTION) {
					unreadToken();
					Error err = funcCall(true);
					if(err != null) return err;
				}
				else {
					unreadToken();
					Error err = assignExpre();
					if(err != null) return err;
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
		return null;
	}
	
	//<跳转语句> -> <返回语句>
	private Error jumpState()  {
		Error err = retState();
		if(err != null) return err;
		return null;
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
			funcOpTable.get(funcNum-1).add(new FuncOption("iret", new Pair()));
			if(err != null) return err;
		}else {
			if(indexTable.get(funcNum-1).getRetType() == IdentiType.VOID) {
				funcOpTable.get(funcNum-1).add(new FuncOption("ret", new Pair()));
			}
			else {
				return new Error(token.getPos(), ErrorType.RUTURN_VALUE_TYPE_ERROR);
			}
		}
		return null;
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
		if(err != null) return err;
		
		token = nextToken();
		if(token.getTokenType() != TokenType.RIGHT_BRACKET)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		err = statement();
		if(err != null) return err;
		
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
			if(err != null) return err;
			setJmpInstruct();
		}
		
		return null;
	}
	
	//<循环语句> -> 'while' '('<状态>')'<语句>
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
		if(err != null) return err;
		
		token = nextToken();
		if(token.getTokenType() != TokenType.RIGHT_BRACKET)
			return new Error(token.getPos(), ErrorType.NO_RIGHT_BRACKET);
		
		err = statement();
		if(err != null) return err;
		funcOpTable.get(funcNum-1).add(new FuncOption("jmp", new Pair(index1)));
		setJmpInstruct();
		return null;
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
		return null;
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
			if(err != null) return err;
			token = nextToken();
			if(token.getTokenType() != TokenType.RIGHT_BRACKET)
				return new Error(token.getPos(), ErrorType.NO_RIGHT_BRACKET);
		}
		token = nextToken();
		if(token.getTokenType() != TokenType.SEMICOLON)
			return new Error(token.getPos(), ErrorType.NO_SEMICOLON_ERROR);
		//输出空格
		funcOpTable.get(funcNum-1).add(new FuncOption("bipush", new Pair('\n')));
		funcOpTable.get(funcNum-1).add(new FuncOption("cprint", new Pair()));
		
		return null;
	}
	
	//<输出列表> -> <输出> { ',' <输出> }
	private Error printList()  {
		Error err = print();
		if(err != null) return err;
		funcOpTable.get(funcNum-1).add(new FuncOption("iprint", new Pair()));
		Token token = nextToken();
		while(token.getTokenType() == TokenType.COMMA) {
			err = print();
			if(err != null) return err;
			funcOpTable.get(funcNum-1).add(new FuncOption("iprint", new Pair()));
			token = nextToken();
		}
		unreadToken();
		return null;
	}
	
	//<输出> -> <表达式>
	private Error print()  {
		Error err = expression();
		if(err != null) return err;
		return null;
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
	private boolean isAbleToAssign2(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		boolean flag = false;
		while(symbolTable.size() != 0) {
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name) 
					&& sb.getType() == IdentiType.INT) {
				flag = true;//可合法赋值
				break;
			}
		}
		while(tmp.size() != 0) {
			symbolTable.push(tmp.pop());//倒回来
		}
		return flag;
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
					&& sb.getType() == IdentiType.INT) {
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
		return ik;//表示没找到
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
		return ik;//表示没找到
	}
	
	//是否是常量的开始
	private boolean isVariableHead() {
		Token token = nextToken();
		if(token.getTokenType() == TokenType.CONST) {
			unreadToken();
			return true;
		}
		else if(token.getTokenType() == TokenType.INT) {
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
	private boolean isFuncHead() {
		Token token = nextToken();
		if(token.getTokenType() == TokenType.VOID) {
			unreadToken();
			return true;
		}
		else if(token.getTokenType() == TokenType.INT) {
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
						index1++;
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
		String jmp2 = new String();
		int a1 = funcOpTable.get(funcNum-1).size() + 2;
		if(s.equals("<")) {
			jmp1 = "jl";
			jmp2 = "jge";
		}
		else if(s.equals("<=")){
			jmp1 = "jle";
			jmp2 = "jg";
		}
		else if(s.equals(">")) {
			jmp1 = "jg";
			jmp2 = "jle";
		}
		else if(s.equals(">=")) {
			jmp1 = "jge";
			jmp2 = "jl";
		}
		else if(s.equals("==")) {
			jmp1 = "je";
			jmp2 = "jne";
		}
		else if(s.equals("!=")) {
			jmp1 = "jne";
			jmp2 = "je";
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
				funcOpTable.get(funcNum-1).get(i).setOperands(new Pair(size));
				break;
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
	
	
	
	
}



