package bot.models;

public class Person {
    private String password;
    private String username;
    private String email;
    private Double balance;
    private boolean confirmed;
    private int id;
    private long chatId;
    private int state;

    public Person(String password, String username, String email, Double balance, boolean confirmed,int id,long chatId,int state) {
        this.password = password;
        this.username = username;
        this.email = email;
        this.balance = balance;
        this.confirmed = confirmed;
        this.id = id;
        this.chatId = chatId;
        this.state = state;
    }

    public Person(){};

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
