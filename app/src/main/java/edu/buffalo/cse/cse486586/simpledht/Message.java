package edu.buffalo.cse.cse486586.simpledht;

public class Message {
    String key;
    String message;

    Message(String k, String msg){
        this.key = k;
        this.message = msg;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static java.util.Comparator<Message> id = new java.util.Comparator<Message>(){
        public int compare(Message m1, Message m2){
            String m1_key = m1.getKey();
            String m2_key = m2.getKey();

            return m1_key.compareTo(m2_key);
        }
    };
}
