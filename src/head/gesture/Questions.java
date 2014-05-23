package head.gesture;

public class Questions {
	
	private String question;
	private String a;
	private String b;
	private String c;
	private String d;
	private Character rightAnswer;
	
	public Questions(String q,String a,String b,String c,String d, Character ra){
		setQuestion(q);
		this.setA(a);
		this.setB(b);
		this.setC(c);
		this.setD(d);
		this.setRightAnswer(ra);
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getA() {
		return a;
	}

	public void setA(String a) {
		this.a = a;
	}

	public String getB() {
		return b;
	}

	public void setB(String b) {
		this.b = b;
	}

	public String getC() {
		return c;
	}

	public void setC(String c) {
		this.c = c;
	}

	public Character getRightAnswer() {
		return rightAnswer;
	}

	public void setRightAnswer(Character rightAnswer) {
		this.rightAnswer = rightAnswer;
	}

	public String getD() {
		return d;
	}

	public void setD(String d) {
		this.d = d;
	}

}
