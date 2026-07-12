package greencloud.impl.managers.alt;

public class Alt {
    private String mask;
    private String data;
    private String refreshToken;
    private final AccountType type;
    private Status status;
    private String statusDetails = "Unknown - Log in to check";

    public Alt(String username) {
        this(username, AccountType.CRACKED);
    }

    public Alt(String data, AccountType type) {
        this(data, "", type, Status.Login);
        if (type == AccountType.CRACKED) this.mask = data;
    }

    public Alt(String data, String mask, AccountType type, Status status) {
        this.data = data;
        this.mask = mask;
        this.type = type;
        this.status = status;
    }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getUsername() { return mask; }
    public void setUsername(String name) { this.mask = name; }

    public AccountType getType() { return type; }

    public boolean isToken() { return this.type == AccountType.TOKEN; }
    public boolean isMicrosoft() { return this.type == AccountType.MICROSOFT; }

    public void setStatus(Status status) { this.status = status; }
    public Status getStatus() { return status; }

    public String getStatusDetails() { return statusDetails; }
    public void setStatusDetails(String details) { this.statusDetails = details; }

    public enum AccountType { CRACKED, TOKEN, MICROSOFT }

    public enum Status {
        Login("\u00A77Login"),
        LoggedIn("\u00A7aLogged In"),
        Banned("\u00A7cBanned"),
        Unknown("\u00A77Unknown");

        public final String label;
        Status(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
}