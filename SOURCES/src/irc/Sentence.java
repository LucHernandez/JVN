package irc;

public class Sentence implements ISentence {
	private String data = "";

	public String read() {
		return data;
	}

	public void write(String s) {
		this.data = s;
	}
}