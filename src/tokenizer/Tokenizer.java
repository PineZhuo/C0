package tokenizer;

import error.ErrorType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.util.ArrayList;

import compiler.Pair;

//�ʷ�������
public class Tokenizer {
	
	private int currentLine = 1;
	private int currentCol = 1;//�����Ŀǰ������λ��
	
//	private Token[] tokenList = new Token()[];
	private ArrayList<Token> tokenList = new ArrayList<>();
	private int index = 0;
	private boolean isFrontZero = false;
	
	public void runTokenizer() throws IOException {
		FileInputStream path = new FileInputStream("d://in.txt");
		PushbackReader input = new PushbackReader(new InputStreamReader(path, "utf8"), 1000);
		//��һ��д��
		/*
		 * int limit = 2; // ��ѡ�����ֻ���ƻ� 2 ���ַ���Ĭ��ֵ�� 1 
		 * File file = new File("D:\\test\\1.txt"); // �ļ������� 123456789
         * PushbackReader reader = new PushbackReader(new FileReader(file), limit);
		 */
        
//		PushbackReader input = new PushbackReader(new InputStreamReader(System.in));
		while(true) {
			Token token = nextToken(input);
			if(token.getTokenType() == TokenType.ERROR) {
				token.printToken();
				break;
			}
			if(token.getTokenType() == TokenType.EOF)
				break;
			token.printToken();
		}
	}
	
	public ArrayList<Token> getTokenList(String in) throws IOException {
		FileInputStream path = new FileInputStream(in);
		PushbackReader input = new PushbackReader(new InputStreamReader(path, "utf8"), 10000);
		while(true) {
			Token token = new Token();
			token = nextToken(input);
			if(token.getTokenType() == TokenType.ERROR) {
				token.printToken();
				return null;
			}
			if(token.getTokenType() == TokenType.EOF) {
				tokenList.add(token);
				break;
			}
			tokenList.add(token);
		}
		return tokenList;
	}
	
	public Token nextToken(PushbackReader input) throws IOException {
		DFAState currentState = DFAState.INITIAL_STATE;
//		int tokenCol = currentCol;//��������token�ĵ�һ����ĸ��col
//		int tokenLine = currentLine;
		Pair pos = new Pair(1, 1);
		pos.setPair(currentLine, currentCol);
		isFrontZero = false;
		String token = new String();
		while(true) {
			Character ch = nextChar(input);
			//�ѷǷ������ɱ��ҡ����
			if(!isPrint(ch))
				return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
//			System.out.println(ch);
			switch(currentState) {
				case INITIAL_STATE:{
					if(ch == null)
						return new Token(pos, TokenType.EOF, token);
					if(isSpace(ch)) {
//						tokenCol = currentCol;
//						tokenLine = currentLine;//�յģ�Ҫ���°�
						pos.setPair(currentLine, currentCol);
//						System.out.println(currentLine+ " " + currentCol);
						continue;
					}
					else if(ch == '0') {//����0
						currentState = DFAState.ZERO_STATE;
					}
					else if(Character.isDigit(ch)) {//��0����,ʮ������
						currentState = DFAState.DEC_STATE;
					}
					else if(Character.isLetter(ch)) {//��ʶ��
						currentState = DFAState.IDENTIFIER_STATE;
					}
					else {
						switch(ch) {
							case '+':
								currentState = DFAState.PLUS_SIGN_STATE;
								break;
							case '-':
								currentState = DFAState.MINUS_SIGN_STATE;
								break;
							case '*':
								currentState = DFAState.MUL_SIGN_STATE;
								break;
							case '/':
								currentState = DFAState.SLASH_STATE;
								break;
							case '=':
								currentState = DFAState.EQUAL_SIGN_STATE;
								break;
							case '<':
								currentState = DFAState.LESS_SIGN_STATE;
								break;
							case '>':
								currentState = DFAState.GREATER_SIGN_STATE;
								break;
							case '!':
								currentState = DFAState.EXCLAMATION_STATE;
								break;
							case '(':
								currentState = DFAState.LEFT_BRACKET_STATE;
								break;
							case ')':
								currentState = DFAState.RIGHT_BRACKET_STATE;
								break;
							case ';':
								currentState = DFAState.SEMICOLON_STATE;
								break;
							case ',':
								currentState = DFAState.COMMA_STATE;
								break;
							case '{':
								currentState = DFAState.LEFT_BRACE_STATE;
								break;
							case '}':
								currentState = DFAState.RIGHT_BRACE_STATE;
								break;
							case '\"':{
								currentState = DFAState.STRING_LITER_STATE;
								break;
							}
							case '\'':{
								currentState = DFAState.CHAR_LITER1_STATE;
								break;
							}
							case '.':{//double������0ֱ�ӿ�ͷ
								currentState = DFAState.DOUBLE_STATE1;
								break;
							}
							default:{
								//���ǺϷ����� �ֲ����ر�ķ��� �Ǿ����ַ��������ַ������
								//����Ҫ���ش���
								System.out.println("����������");
								return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
							}	
						}
					}
					//DFA���ǳ�ʼ״̬��
					//�϶����ǰ���
					//ֱ�ӰѶ������ַ����ӵ�token��
					//�ַ����� \" ���ü�
					if(currentState != DFAState.STRING_LITER_STATE && currentState != DFAState.CHAR_LITER1_STATE) {
						token += ch;
					}
					break;
				}
				case ZERO_STATE:{
					if(ch == null) {//�ļ�β
						return new Token(pos, TokenType.DEC_INTEGER, token);
					}
					//ʮ������
					if(ch == 'x' || ch == 'X') {
						currentState = DFAState.HEX_STATE;
						token += ch;
					}
					else if(Character.isDigit(ch)) {//��ǰ��0��ʮ���ƣ�����x������Ҫдdouble�����ﲻ�ܱ�����
//						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
						isFrontZero = true;
						token += ch;
						currentState = DFAState.DEC_STATE;
					}
					else if(ch == '.') {
						token += ch;
						currentState = DFAState.DOUBLE_STATE6;
					}
					else {//����0
						unread(input, ch);
						return new Token(pos, TokenType.DEC_INTEGER, token);
					}
					break;
				}
				case DEC_STATE:{
					if(ch == null) {
						int a = 0;
						try { 
							a = Integer.parseInt(token);
						}
						catch(NumberFormatException e) {
							return new Token(pos, TokenType.ERROR, ErrorType.TOO_LARGE_INTEGER);
						}
						if(isFrontZero == false)
							return new Token(pos, TokenType.DEC_INTEGER, Integer.toString(a));
						else
							return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					if(Character.isDigit(ch)) {
						token += ch;
					}
					else if(ch == 'e' || ch == 'E') {//��������ĸ��������ζ��double
						token += ch;
						currentState = DFAState.DOUBLE_STATE3;
					}
					else if(ch == '.') {//double
						currentState = DFAState.DOUBLE_STATE6;
						token += ch;
					}
					else if(Character.isLetter(ch)) {//��ĸ������
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					else{
						unread(input, ch);//����
						int a = 0;
						try {
							a = Integer.parseInt(token);
						}
						catch(NumberFormatException e) {
							//e.printStackTrace();
							return new Token(pos, TokenType.ERROR, ErrorType.TOO_LARGE_INTEGER);
						}
//						return new Token(pos, TokenType.DEC_INTEGER, Integer.toString(a));//�������token
						if(isFrontZero == false)
							return new Token(pos, TokenType.DEC_INTEGER, Integer.toString(a));
						else
							return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					break;
				}
				case DOUBLE_STATE1:{
					if(ch == null) {
//						double d = 0;
//						try { 
//							d = Double.parseDouble(token);
//						}catch(NumberFormatException e) {
//							return new Token(pos, TokenType.ERROR, ErrorType.TOO_LARGE_DOUBLE);
//						}
//						return new Token(pos, TokenType.DOUBLE, token);
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					
					if(Character.isDigit(ch)) {
						token += ch;
						currentState = DFAState.DOUBLE_STATE2;
					}
					else {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					break;
				}
				case DOUBLE_STATE2:{
					if(ch == null) {
						Double d = Double.valueOf(token);
						if(d.isInfinite() || d.isNaN())
							return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
						return new Token(pos, TokenType.DOUBLE_DIGIT, token);	
					}
					
					if(Character.isDigit(ch)) {
						token += ch;
					}
					else if(ch == 'e' || ch == 'E') {
						token += ch;
						currentState = DFAState.DOUBLE_STATE3;
					}
					else {
						unread(input, ch);
						Double d = Double.valueOf(token);
						if(d.isInfinite() || d.isNaN())
							return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
						return new Token(pos, TokenType.DOUBLE_DIGIT, token);	
					}
					break;
				}
				case DOUBLE_STATE3:{
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					if(ch == '+' || ch == '-') {
						token += ch;
						currentState = DFAState.DOUBLE_STATE4;
					}
					else {//���˼Ӻͼ����⣬��������һ���׶��ж�
						unread(input, ch);
						currentState = DFAState.DOUBLE_STATE4;
					}
					break;
				}
				case DOUBLE_STATE4:{
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					if(Character.isDigit(ch)) {
						token += ch;
						currentState = DFAState.DOUBLE_STATE5;
					}
					else {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					break;
				}
				case DOUBLE_STATE5:{
					if(ch == null) {
						Double d = Double.valueOf(token);
						if(d.isInfinite() || d.isNaN())
							return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
						return new Token(pos, TokenType.DOUBLE_DIGIT, token);
					}
					if(Character.isDigit(ch)) {
						token += ch;
						currentState = DFAState.DOUBLE_STATE5;
					}
					else {
						unread(input, ch);
						Double d = Double.valueOf(token);
						if(d.isInfinite() || d.isNaN())
							return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
						return new Token(pos, TokenType.DOUBLE_DIGIT, Double.toString(d));
					}
					break;
				}
				case DOUBLE_STATE6:{
					if(ch == null) {
						Double d = Double.valueOf(token);
						if(d.isInfinite() || d.isNaN())
							return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
						return new Token(pos, TokenType.DOUBLE_DIGIT, token);
					}
					if(ch == 'e' || ch == 'E') {
						token += ch;
						currentState = DFAState.DOUBLE_STATE3;
					}
					else if(Character.isDigit(ch)) {
						token += ch;
						currentState = DFAState.DOUBLE_STATE2;
					}
					else{
						unread(input, ch);
						Double d = Double.valueOf(token);
						if(d.isInfinite() || d.isNaN())
							return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
						return new Token(pos, TokenType.DOUBLE_DIGIT, Double.toString(d));
					}
					break;
				}
				case HEX_STATE:{
					if(ch == null) {
						if(token.length() == 2)//����Ϊ2�ͽ����ˣ�����
							return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
						else {
							int a = 0;
							token = token.substring(2);
							try { 
								a = Integer.valueOf(token, 16);
							}
							catch(NumberFormatException e) {
								return new Token(pos, TokenType.ERROR, ErrorType.TOO_LARGE_INTEGER);
							}
							return new Token(pos, TokenType.DEC_INTEGER, Integer.toString(a));
						}
					}
					if(isHexChar(ch)) {
						token += ch;
					}
					else {
						unread(input, ch);//����
						if(token.length() == 2)
							return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
						else {
							int a = 0;
							token = token.substring(2);
							try { 
								a = Integer.valueOf(token, 16);
							}
							catch(NumberFormatException e) {
								return new Token(pos, TokenType.ERROR, ErrorType.TOO_LARGE_INTEGER);
							}
							return new Token(pos, TokenType.DEC_INTEGER, Integer.toString(a));
						}
					}
					break;
				}
				case IDENTIFIER_STATE:{
					if(ch == null) {
						return new Token(pos, getIdenType(token), token);
					}
					if(Character.isDigit(ch) || Character.isLetter(ch)) {
						token += ch;
					}
					else {
						unread(input, ch);
						return new Token(pos, getIdenType(token), token);
					}
					break;
				}
				case PLUS_SIGN_STATE:{
					//������ʲô�����ǻ���Ȼ�󷵻�token
					if(ch != null)
						unread(input, ch);
					return new Token(pos, TokenType.PLUS_SIGN, token);
				}
				case MINUS_SIGN_STATE:{
					if(ch != null)
						unread(input, ch);
					return new Token(pos, TokenType.MINUS_SIGN, token);
				}
				case MUL_SIGN_STATE:{
					if(ch != null)
						unread(input, ch);
					return new Token(pos, TokenType.MUL_SIGN, token);
				}
				case SLASH_STATE:{
					if(ch == null)
						return new Token(pos, TokenType.DIV_SIGN, token);//����
					
					if(ch == '*') {
						currentState = DFAState.ANNOTATION_STAR1_STATE;//ע��
						token = "";
						break;
					}
					else if(ch == '/') {
						currentState = DFAState.ANNOTATION_SLASH_STATE;//ע��
						token = "";
						break;
					}
					else {
						unread(input, ch);
						return new Token(pos, TokenType.DIV_SIGN, token);//����
					}
					
				}
				case ANNOTATION_STAR1_STATE:{
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.ANNOTATION_ERROR);
					}						
					if(ch != '*')//ע���е�����
						break;
					else if(ch == '*') {
						currentState = DFAState.ANNOTATION_STAR2_STATE;
						break;
					}
				}
				case ANNOTATION_STAR2_STATE:{
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.ANNOTATION_ERROR);
					}
					if(ch == '/') {
						currentState = DFAState.INITIAL_STATE;
					}
					else {
						currentState = DFAState.ANNOTATION_STAR1_STATE;//�Ҵ��ˣ����*���治��б��
						//ֱ���ҵ�*/���ע�Ͳ���ͣ ����ҵ����û�ҵ� �ͱ���
//						return new Token(pos, TokenType.ERROR, ErrorType.ANNOTATION_ERROR);
					}
					break;
				}
				case ANNOTATION_SLASH_STATE:{
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.ANNOTATION_ERROR);
					}
						
					if(ch == 0x0a || ch == 0x0d) {
						currentState = DFAState.INITIAL_STATE;
						break;
					}else {
						break;
					}
				}
				case EQUAL_SIGN_STATE:{
					if(ch == null) {
						return new Token(pos, TokenType.EQUAL_SIGN, token);
					}
					if(ch == '=') {//�����Ⱥţ�"=="
						token += ch;
						return new Token(pos, TokenType.DOUBLE_EQUAL_SIGN, token);
					}
					else {
						unread(input, ch);
						return new Token(pos, TokenType.EQUAL_SIGN, token);
					}
				}
				case LESS_SIGN_STATE:{
					if(ch == null) {
						return new Token(pos, TokenType.LESS_SIGN, token);
					}
					if(ch == '=') {
						token += ch;
						return new Token(pos, TokenType.LESS_OR_EQUAL_SIGN, token);
					}
					else {
						unread(input, ch);
						return new Token(pos, TokenType.LESS_SIGN, token);
					}
				}
				case GREATER_SIGN_STATE:{
					if(ch == null) {
						return new Token(pos, TokenType.GREATER_SIGN, token);
					}
					if(ch == '=') {
						token += ch;
						return new Token(pos, TokenType.GREATER_OR_EQUAL_SIGN, token);
					}
					else {
						unread(input, ch);
						return new Token(pos, TokenType.GREATER_SIGN, token);
					}
				}
				case EXCLAMATION_STATE:{
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					if(ch == '=') {
						token += ch;
						return new Token(pos, TokenType.NOT_EQUAL_SIGN, token);
					}
					else {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
				}
				case LEFT_BRACKET_STATE:{
					if(ch != null)
						unread(input, ch);
					return new Token(pos, TokenType.LEFT_BRACKET, token);
				}
				case RIGHT_BRACKET_STATE:{
					if(ch != null)
						unread(input, ch);
					return new Token(pos, TokenType.RIGHT_BRACKET, token);
				}
				case SEMICOLON_STATE:{
					if(ch != null)
						unread(input, ch);
					return new Token(pos, TokenType.SEMICOLON, token);
				}
				case COMMA_STATE:{
					if(ch != null)
						unread(input, ch);
					return new Token(pos, TokenType.COMMA, token);
				}
				case LEFT_BRACE_STATE:{
					if(ch != null)
						unread(input, ch);
					return new Token(pos, TokenType.LEFT_BRACE, token);
				}
				case RIGHT_BRACE_STATE:{
					if(ch != null)
						unread(input, ch);
					return new Token(pos, TokenType.RIGHT_BRACE, token);
				}
				case CHAR_LITER1_STATE:{// '
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					if(ch == '\\') {//˵������һ��<escape-seq>�������
						currentState = DFAState.CHAR_ESCAPE_LITER_STATE;
					}
					else if(ch == '\'') {//�൱��û���ַ������������ĵ�����
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					else{
						token += ch;
						currentState = DFAState.CHAR_LITER2_STATE;
					}
					break;
				}
				case CHAR_LITER2_STATE:{// 'a    '\'   '\xab
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					if(ch == '\'') {
						return new Token(pos, TokenType.CHARACTER, token);
					}
					else {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
				}
				case CHAR_ESCAPE_LITER_STATE:{// '\
					
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					if(isEscape(ch)) {
						if(ch == 'x') {
							currentState = DFAState.CHAR_HEX_LITER1_STATE;
						}
						else if(ch == '\'' || ch == '\"' || ch == '\\'){
							token += ch;
							currentState = DFAState.CHAR_LITER2_STATE;
						}
						else if(ch == 'n') {
							token += '\n';
							
							currentState = DFAState.CHAR_LITER2_STATE;
						}
						else if(ch == 't') {
							token += '\t';
							currentState = DFAState.CHAR_LITER2_STATE;
						}
						else if(ch == 'r') {
							token += '\r';
							currentState = DFAState.CHAR_LITER2_STATE;
						}
					}
					else {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					break;
				}
				case CHAR_HEX_LITER1_STATE:{
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					if(isHexChar(ch)) {
						token += ch;
						currentState = DFAState.CHAR_HEX_LITER2_STATE;
					}
					else {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					break;
				}
				case CHAR_HEX_LITER2_STATE:{
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					if(isHexChar(ch)) {
						token += ch;
						int a = Integer.valueOf(token, 16);
						token = "";
						token += (char)a;
						currentState = DFAState.CHAR_LITER2_STATE;
					}
					else {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					break;
				}
				case STRING_LITER_STATE:{
					//�ַ���û�н��� ����������������
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					if(ch == '\"') {//����
						return new Token(pos, TokenType.STRING, token);
					}
					else if(ch == '\\') {
						currentState = DFAState.STRING_ESCAPE_LITER;
					}
					else {
						token += ch;
					}
					break;
				}
				case STRING_ESCAPE_LITER:{
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					if(isEscape(ch)) {
						if(ch == 'x') {
							currentState = DFAState.STRING_HEX_LITER1_STATE;
						}
						else if(ch == '\"' || ch == '\'' || ch == '\\') {
							token += ch;
							currentState = DFAState.STRING_LITER_STATE;
						}
						else if(ch == 'n') {
							token += '\n';
							currentState = DFAState.STRING_LITER_STATE;
						}
						else if(ch == 'r') {
							token += '\r';
							currentState = DFAState.STRING_LITER_STATE;
						}
						else if(ch == 't') {
							token += '\t';
							currentState = DFAState.STRING_LITER_STATE;
						}
					}
					else {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					break;
				}
				case STRING_HEX_LITER1_STATE:{
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					if(isHexChar(ch)) {
						token += ch;
						currentState = DFAState.STRING_HEX_LITER2_STATE;
					}
					else {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					break;
				}
				case STRING_HEX_LITER2_STATE:{
					if(ch == null) {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					if(isHexChar(ch)) {
						token += ch;
//						System.out.println(token);
						int a = Integer.valueOf(token.substring(token.length()-2, token.length()), 16);
						token = token.substring(0, token.length()-2);
						token += (char)a;
						currentState = DFAState.STRING_LITER_STATE;
					}
					else {
						return new Token(pos, TokenType.ERROR, ErrorType.INVALID_INPUT_ERROR);
					}
					break;
				}
			default:
				break;
			}
		}
	}	
	
	//��ȡһ���ַ�
	private Character nextChar(PushbackReader input) {
		int x = 0;
		try {
			x = input.read();					
		} catch (IOException e) {
			e.printStackTrace();
		}	
		currentCol ++;
		if(x == -1)
			return null;
		if(x == 0x0a) {
			//����
			currentLine ++;//��++
			currentCol = 1;//������
		}
		char c = (char)x;
		return c;
	}
	
	private void unread(PushbackReader input, char ch) throws IOException {
		input.unread(ch);
		if(currentCol == 1) {
			currentLine --;//���˻���һ�У�col����
		}
		else
			currentCol --;
	}
	
	//�Ƿ�Ϊ�հ׷�
	private boolean isSpace(char c) {
		int x = (int)c;
		if(x == 0x20 || x == 0x09 || x == 0x0a || x == 0x0d)
			return true;
		else
			return false;
	}
	
	//�Ƿ�Ϊ�Ϸ�����
	private boolean isPrint(Character c) {
		if(c == null || Character.isDigit(c) ||
				Character.isLetter(c) ||
				isSpace(c) ||
				c == '(' ||c == ')' ||c == '{' ||c == '}'
				||c == '<' ||c == '=' ||c == '>'
				||c == ';' ||c == ',' ||c == '!'
				||c == '+' ||c == '-' ||c == '*' ||c == '/'
				||c == '_' ||c == '[' ||c == ']' 
				||c == '.' ||c == ':' ||c == '?' ||c == '%'
				||c == '^' ||c == '&' ||c == '|' ||c =='~'
				||c == '\\' ||c == '\"' ||c == '\'' 
				||c == '`' ||c == '$' ||c == '#' ||c == '@') {
			return true;
		}
		else
			return false;
	}
	
	private boolean isHexChar(char c) {
		if(Character.isDigit(c) 
				|| c == 'a' || c == 'A'
				|| c == 'b' || c == 'B'
				|| c == 'c' || c == 'C'
				|| c == 'd' || c == 'D'
				|| c == 'e' || c == 'E'
				|| c == 'f' || c == 'F') {
			return true;
		}
		else
			return false;
	}
	
	private TokenType getIdenType(String s) {
		if(s.equals("const")) return TokenType.CONST;
		else if(s.equals("void")) return TokenType.VOID;
		else if(s.equals("int")) return TokenType.INT;
		else if(s.equals("char")) return TokenType.CHAR;
		else if(s.equals("double")) return TokenType.DOUBLE;
		else if(s.equals("struct")) return TokenType.STRUCT;
		else if(s.equals("if")) return TokenType.IF;
		else if(s.equals("else")) return TokenType.ELSE;
		else if(s.equals("switch")) return TokenType.SWITCH;
		else if(s.equals("case")) return TokenType.CASE;
		else if(s.equals("default")) return TokenType.DEFAULT;
		else if(s.equals("while")) return TokenType.WHILE;
		else if(s.equals("for")) return TokenType.FOR;
		else if(s.equals("do")) return TokenType.DO;
		else if(s.equals("return")) return TokenType.RETURN;
		else if(s.equals("break")) return TokenType.BREAK;
		else if(s.equals("continue")) return TokenType.CONTINUE;
		else if(s.equals("print")) return TokenType.PRINT;
		else if(s.equals("scan")) return TokenType.SCAN;
		else return TokenType.IDENTIFIER;
	}
	
	//�ж�һ���ַ��ǲ��� <escape-seq> �ﶨ���
	private boolean isEscape(char ch) {
		if(ch == '\\' || ch == '\'' || ch == '\"'
				|| ch == 'n' || ch == 'r' || ch == 't'
				|| ch == 'x') {
			return true;
		}
		else
			return false;
	}
	
}


