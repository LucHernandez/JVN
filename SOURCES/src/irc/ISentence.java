package irc;

import java.io.Serializable;
import jvn.JvnRead;
import jvn.JvnWrite;

public interface ISentence extends Serializable {

    @JvnRead
    public String read();

    @JvnWrite
    public void write(String s);
}