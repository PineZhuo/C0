package tokenizer;

public enum TokenType {
	//保留字
	CONST,
	VOID,
	INT,
	CHAR,
	DOUBLE,
	STRUCT,
	IF,
	ELSE,
	SWITCH,
	CASE,
	DEFAULT,
	WHILE,
	FOR,
	DO, 
	RETURN,
	BREAK,
	CONTINUE,
	PRINT,
	SCAN,
	CHARACTER,//字符，不是char关键字
	STRING,//字符串
	//十进制整数
	DEC_INTEGER, 
	INTEGER,
	//double
	DOUBLE_DIGIT,
	//十六进制整数
	HEX_INTEGER,
	//标识符
	IDENTIFIER,
	//运算符
	PLUS_SIGN,
	MINUS_SIGN,
	MUL_SIGN,
	DIV_SIGN,
	LESS_SIGN,
	LESS_OR_EQUAL_SIGN,
	GREATER_SIGN,
	GREATER_OR_EQUAL_SIGN,
	NOT_EQUAL_SIGN,
	DOUBLE_EQUAL_SIGN,
	EQUAL_SIGN,
	//其他符号
	LEFT_BRACKET,
	RIGHT_BRACKET,
	LEFT_BRACE,//大括号
	RIGHT_BRACE,
	SEMICOLON,
	COMMA,//逗号
	//错误
	ERROR,
	//文件末尾，不算出错吧
	EOF,
}
