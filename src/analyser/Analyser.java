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
	//�Ӵʷ������õ���tokenList���±�
	private int index = 0;
	//�ʷ������Ľ��
	private ArrayList<Token> tokenList = new ArrayList<>();
//	private int tokenNum;
	//�����ĸ���
	private int funcNum = 0;
	//����������
	private ArrayList<IndexTable> indexTable = new ArrayList<>();
	private ArrayList<Pair> paraList = new ArrayList<>();
	//����һ�������žͼ�һ��
	private int level = 0;
	//ȫ�ֵ�ջʽ���ű�
	private Stack<Symbol> symbolTable = new Stack<Symbol>();
	//һ���������㺯������ʱ����������ȫ�ֱ���
	private int paraNum = 0;
	//��������±�
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
		if(tokenList == null) //˵���ʷ�����������
			return ;
		
		//����̨����ĵ��ļ���
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
	
	// <����> -> {��������}{��������}
	// ���ؿձ�ʾû�д���
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
	// <��������> -> [const] <����><��ʼ��˵���б�>';'
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
	
	
	//<��ʼ��˵���б�> -> <��ʼ��˵��>{','<��ʼ��˵��>}
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
	
	//<��ʼ��˵��> -> <��ʶ��> [<��ʼ��>]
	// a
	// b = 1
	private Error initDec(boolean isConst, TokenType tt)  {
		Token token = nextToken();
		if(token.getTokenType() == TokenType.IDENTIFIER) {
			//�˴��������ű�
			//����ظ����� ����
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
			
			//����δ����ֵ
			if(isConst && token.getTokenType() != TokenType.EQUAL_SIGN) {
				return new Error(token.getPos(), ErrorType.CONSTANT_NEED_VALUE);
			}
			
			//�������߱���������ֵ
			if(token.getTokenType() == TokenType.EQUAL_SIGN) {
				unreadToken();
				Error err = init(tt);
				if(err != null) return err;
			}
			else {
				//����ֵ0
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
	
	//<��ʼ��> -> ��=�� <���ʽ>
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
	
	// <��ֵ���ʽ> -> <��ʶ��>[ '=' <���ʽ>]
	private Error assignExpre()  {
		Token token = nextToken();
		if(token.getTokenType() != TokenType.IDENTIFIER)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		String name = token.getValue();
		
		//�еȺ���Ҫ��û�еȺŶ�һ������->���ǳ���
		if(!isAbleToAssign1(name))
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		token = nextToken();
		if(token.getTokenType() == TokenType.EQUAL_SIGN) {
			//�еȺ� �����ǲ��ǳ���
			if(!isAbleToAssign2(name)) {
				return new Error(getLastToken().getPos(), ErrorType.INVALID_ASSIGNMENT);
			}
			//����name�ҵ���ջ�е�ƫ��
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
	
	// <״̬> -> <���ʽ>[<�ȽϷ���> <���ʽ>]
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
	
	// <���ʽ> -> <���ʽ���>
	private Error expression()  {
		Error err = addExpre();
		if(err != null) return err;
		return null;
	}
	
	// <���ʽ���> -> <��> { <�Ӽ�����> <��>}
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
	
	// <��> -> <һԪ���ʽ> { <�˳�����> <һԪ���ʽ>}
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
		unreadToken();//֮ǰ�����һ�����ǣ���-�ģ�����
		return null;
	}
	
	// <һԪ���ʽ> -> [<һԪ������>] <��Ҫ���ʽ>
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
		}//�������ɵ�ʱ������
		else {
			unreadToken();//����
			Error err = priExpre();
			if(err != null) return err;
		}
		return null;
	}
	
	// <��Ҫ���ʽ> -> '(' <���ʽ> ')' | <��ʶ��> | <����> | <��������>
	private Error priExpre()  {
		Token token = nextToken();
		
		switch(token.getTokenType()) {
			case LEFT_BRACKET:{
				Error err = expression();
				if(err == null) {
					//Ϊ�� ֤��������ȷ�� ���Ǽ����ж�������
					token = nextToken();
					if(token.getTokenType() == TokenType.RIGHT_BRACKET)
						return null;//��ȷ ���ؿ�
					else
						return new Error(token.getPos(), ErrorType.NO_RIGHT_BRACKET);
				}
				else {
					return err;
				}
			}
			case IDENTIFIER:{//���ݱ�ʶ�������ķ��ű��ж��Ǳ�ʶ�����Ǻ�������
				//���ж��Ƿ��壬ֱ���ڷ��ű�����
				//���ж���������
				//����Ǻ��� Ҫ�жϷ���ֵ�ǲ���void ����Ǳ�ʶ�� Ҫ�ж������ǲ���void
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
				else {//�����Ͳ�������ȡ��ַ������ֵ
					//���ж��Ƿ���void
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
	
	//<��������> -> <����><��ʶ��><����><�ϳ����>
	@SuppressWarnings("unchecked")
	private Error funcDef() {
		//����
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
		//��ʶ�� Ҳ��������
		token = nextToken();
		if(token.getTokenType() != TokenType.IDENTIFIER)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		String name = token.getValue();
		//���������ű�
		if(isSecondName(token.getValue()))
			return new Error(token.getPos(), ErrorType.DUPLICATE_DECLARATION);
		symbolTable.push(new Symbol(token, it, IdentiKind.FUNCTION, level));
		
		//����
		paraList.clear();
		Error err = paraClause();
		if(err != null) return err;
		//������
		constTable.add(new Constant(constIndex, "S", name));
		//������
		funcTable.add(new Functions(funcNum, constIndex, paraList.size()));
		constIndex++;
		ArrayList<Pair> paralist = new ArrayList<>();
		paralist = (ArrayList<Pair>)paraList.clone();
		indexTable.add(new IndexTable(name, it, paralist));
		funcOpTable.add(new ArrayList<FuncOption>());
		funcNum ++;
		err = compoundState();
		if(err != null) return err;
		//���Լӷ���ָ��
		if(it == IdentiType.VOID) {
			funcOpTable.get(funcNum-1).add(new FuncOption("ret", new Pair()));
		}
		else {//����һ��0
			funcOpTable.get(funcNum-1).add(new FuncOption("ipush", new Pair(0)));
			funcOpTable.get(funcNum-1).add(new FuncOption("iret", new Pair()));
		}
		return null;
	}
	
	//<����> -> '(' [<���������б�>] ')'
	private Error paraClause() {
		Token token = nextToken();
		if(token.getTokenType() != TokenType.LEFT_BRACKET)
			return new Error(token.getPos(), ErrorType.NO_LEFT_BRACKET);
		
		//����������һ��������
		level++;
	
		token = nextToken();
		unreadToken();//Ԥ��
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
	
	//<���������б�> -> <��������>{ ',' <��������>}
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
	
	//<��������> -> [<const>]<����> <��ʶ��>
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
		
		
		//���������ű�,����ҲҪ�ж��Ƿ�ͱ�����������,��Ϊ�п��ܲ���������
		if(isSecondName(token.getValue()))
			return new Error(token.getPos(), ErrorType.DUPLICATE_DECLARATION);
		//��������Ҫ���²���
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
	
	//<��������> -> <��ʶ��> '(' [<���ʽ�б�>] ')'
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
		
		//���ﻹû���жϲ����Ƿ���ͬ ������̫���� �Ժ���д ������
		//�ٺٺ�,����C0�Ĳ���ֻ��int��const int���������������ְ��ٺٺ�
		//����Ŀǰֻ��Ҫ�ж������Ƿ�һ��
		paraNum = 0;//����
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
		
		//�����Щ��û�� ���Ҿ�call
		funcOpTable.get(funcNum-1).add(new FuncOption("call", new Pair(index1)));
		//����ǿ� ������û�з���ֵ �Ͳ�Ҫpop��
		if(isPop && indexTable.get(index1).getRetType() != IdentiType.VOID) {
			funcOpTable.get(funcNum-1).add(new FuncOption("pop", new Pair()));
		}
		return null;
	}
	
	//<���ʽ�б�> -> <���ʽ> { ','<���ʽ> }
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
	
	//<�ϳ����> -> '{' {<��������>}<�������> '}'
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
		
		//��ǰ�㼶��ȫ����ջ
		while(symbolTable.peek().getLevel() == level) {
//			symbolTable.peek().printSymbol();
			symbolTable.pop();
		}
		level--;
		
		return null;
	}
	
	//<�������> -> {<���>}
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
	
	//<���> -> '{' <�������> '}' | <�������> | <ѭ�����>
	// | <��ת���> | <������> | <�������> 
	// | <��ֵ���ʽ> ';' | <��������> ';' | ';' 
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
				
				//��ǰ�㼶��ȫ����ջ
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
				//�÷��ű��һ���ҵ��������ж���������ɶ
				IdentiKind ik = isAlreadyDec(token.getValue());
				//δ�������ܣ�������ֵ���ͺ��������Լ�����
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
	
	//<��ת���> -> <�������>
	private Error jumpState()  {
		Error err = retState();
		if(err != null) return err;
		return null;
	}
	
	//<�������> -> 'return' [<���ʽ>] ';'
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
	
	//<�������> -> 'if' '(' <״̬> ')'<���> ['else'<���>]
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
			//����Ҫ�ڵ�ǰ������table���ҵ������ݴ��������ת���
			//Ȼ���޸���
			setJmpInstruct();
			unreadToken();
		}
		else {
			funcOpTable.get(funcNum-1).add(new FuncOption("jmp", new Pair()));//if������ˣ��Ͳ���else��
			setJmpInstruct();
			err = statement();
			if(err != null) return err;
			setJmpInstruct();
		}
		
		return null;
	}
	
	//<ѭ�����> -> 'while' '('<״̬>')'<���>
	private Error loopState()  {
		Token token = nextToken();
		if(token.getTokenType() != TokenType.WHILE)
			return new Error(token.getPos(), ErrorType.INVALID_INPUT_ERROR);
		
		token = nextToken();
		if(token.getTokenType() != TokenType.LEFT_BRACKET)
			return new Error(token.getPos(), ErrorType.NO_LEFT_BRACKET);
		
		//�⽫���Ϊcondition���һ��ָ����±�
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
	
	//<�������> -> 'scan' '('<��ʶ��>')' ';'
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
		//scan�ı�ʶ�������Ƿ�constֵ���������ֵ������ڣ��Ҳ��Ǻ���
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
	
	//<������> -> 'print' '('[<����б�>]')'';'
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
		//����ո�
		funcOpTable.get(funcNum-1).add(new FuncOption("bipush", new Pair('\n')));
		funcOpTable.get(funcNum-1).add(new FuncOption("cprint", new Pair()));
		
		return null;
	}
	
	//<����б�> -> <���> { ',' <���> }
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
	
	//<���> -> <���ʽ>
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
	
	//�жϱ�ʶ�������ڱ��㼶���Ƿ��ظ�
	private boolean isSecondName(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		boolean flag = false;
		while(symbolTable.size() != 0 && symbolTable.peek().getLevel() == level) {
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name)) {
				flag = true;//�ظ���
				 break;
			}
		}
		while(tmp.size() != 0) {
			symbolTable.push(tmp.pop());//������
		}
		return flag;
	}
	
	//�ж��Ƿ�ɸ�ֵ Ҳ�� �Ƿ������� �Ƿ��Ǳ���/����
	private boolean isAbleToAssign1(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		boolean flag = false;
		while(symbolTable.size() != 0) {//���������ҵ��ĵ�һ��ͬ���ľ���
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name) 
					&& (sb.getKind() == IdentiKind.VARIABLE || sb.getKind() == IdentiKind.PARAMETER)) {
				flag = true;//�ɺϷ���ֵ
				break;
			}
		}
		while(tmp.size() != 0) {
			symbolTable.push(tmp.pop());//������
		}
		return flag;
	}
	
	//ǰ�����Ѿ�����1���ж���
	private boolean isAbleToAssign2(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		boolean flag = false;
		while(symbolTable.size() != 0) {
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name) 
					&& sb.getType() == IdentiType.INT) {
				flag = true;//�ɺϷ���ֵ
				break;
			}
		}
		while(tmp.size() != 0) {
			symbolTable.push(tmp.pop());//������
		}
		return flag;
	}
	
	//�ж���������Ƿ���� ������ �Ƿ��Ǳ������߿ɱ���� ��scan��
	private boolean isAbleToAssign(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		boolean flag = false;
		while(symbolTable.size() != 0) {
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name) 
					&& (sb.getKind() == IdentiKind.PARAMETER || sb.getKind() == IdentiKind.VARIABLE)
					&& sb.getType() == IdentiType.INT) {
				flag = true;//�ɺϷ���ֵ
				break;
			}
		}
		while(tmp.size() != 0) {
			symbolTable.push(tmp.pop());//������
		}
		return flag;
	}
	
	//�жϺ����Ƿ��Ѿ�����
	//�ҵ���������ں���������±�
	//δ�����ͷ���-1
	private int isFuncBeenDec(String name) {
		for(int i = 0; i < funcNum; i++) {
			if(name.equals(indexTable.get(i).getName())) {
				return i;
			}
		}
		return -1;
	}
	
	
	//�жϸñ�ʶ�������ͣ���������������������
	//�����ʶ��δ����������null
	private IdentiKind isAlreadyDec(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		IdentiKind ik = null;
		while(symbolTable.size() != 0) {//���������ҵ��ĵ�һ��ͬ���ľ���
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
			symbolTable.push(tmp.pop());//������
		}
		return ik;//��ʾû�ҵ�
	}
	
	private IdentiKind isAlreadyDecAndNoVoid(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		IdentiKind ik = null;
		while(symbolTable.size() != 0) {//���������ҵ��ĵ�һ��ͬ���ľ���
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name)) {
				if(sb.getType() == IdentiType.CONST_VOID || sb.getType() == IdentiType.VOID) {
					//��Ȼ�ҵ��� ����������void û��
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
			symbolTable.push(tmp.pop());//������
		}
		return ik;//��ʾû�ҵ�
	}
	
	//�Ƿ��ǳ����Ŀ�ʼ
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
					//�����������ŵ�����һ�ɵ������������д�Ļ�����ᷢ��
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
	
	//�Ƿ��Ǻ����Ŀ�ʼ
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
	
	//�ж��Ƿ���main����
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
	
	//�ҵ�����������������еĶ���
	private IndexTable findFunc(String name) {
		int size = indexTable.size();
		for(int i = 0; i < size; i++) {
			if(indexTable.get(i).getName().equals(name)) {
				return indexTable.get(i);
			}
		}
		return null;
	}
	
	//ͨ����ʶ�������� �ұ�ʶ���ĺ���Ƕ�׵Ĳ㼶����±�
	//�ڷ��ű�����
	//֮ǰ�Ѿ��жϹ��ˣ��������һ�������
	private Pair getLevelandIndex(String name) {
		Stack<Symbol> tmp = new Stack<Symbol>();
		int index1 = 0;
		int level1 = 0;
		while(symbolTable.size() != 0) {//���������ҵ��ĵ�һ��ͬ���ľ���
			Symbol sb = symbolTable.pop();
			tmp.push(sb);
			if(sb.getName().equals(name)) {
				//��������ʶ���Ĳ㼶
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
			symbolTable.push(tmp.pop());//������
		}
		return new Pair(level1, index1);
	}
	
	//�����תָ��
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
	
	//Ϊ֮ǰ���յ���תָ������offset
	private void setJmpInstruct() {
		ArrayList<FuncOption> tmp = funcOpTable.get(funcNum-1);
		int size = funcOpTable.get(funcNum-1).size();
		for(int i = size-2; i >= 0; i--) {
			if(isCompareInstruct(tmp.get(i).getOpcode())) {
				//�ҵ��˵�һ����ת
				funcOpTable.get(funcNum-1).get(i).setOperands(new Pair(size));
				break;
			}
		}
	}
	
	//�ж��Ƿ�����תָ��
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



