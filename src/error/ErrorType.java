package error;

//��������
public enum ErrorType {
	INVALID_INPUT_ERROR,
	TOO_LARGE_INTEGER,

	NO_SEMICOLON_ERROR,
	NO_LEFT_BRACKET,
	NO_RIGHT_BRACKET,
	NO_LEFT_BRACE,
	NO_RIGHT_BRACE,
	
	CONSTANT_NEED_VALUE,//����δ��ֵ
	
	// �ظ�����
	DUPLICATE_DECLARATION,
	//�Ƿ��ĸ�ֵ
	INVALID_ASSIGNMENT,
	//δ����
	NO_DECLARED,
	//�����͵���ʱ��ͬ
	PARAMETER_TYPE_ERROR,
	//��ֵ���ʽ������void����ֵ
	CANNOT_ASSIGN_VOID,
	//ע��
	ANNOTATION_ERROR,
	//����ֵ���ʹ���
	RUTURN_VALUE_TYPE_ERROR,
}
