 

/**
 * Account class supporting users and administrators
 */
public class Account {
    private String username;
    private String password;
    private AccountRole role;
    private String email;
    private String fullName;
    private String contactNumber;

    public Account(String username, String password, AccountRole role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = "";
        this.fullName = "";
        this.contactNumber = "";
    }
    
    public Account(String username, String password, AccountRole role, String email) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email != null ? email : "";
        this.fullName = "";
        this.contactNumber = "";
    }
    
    public Account(String username, String password, AccountRole role, String email, String fullName, String contactNumber) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email != null ? email : "";
        this.fullName = fullName != null ? fullName : "";
        this.contactNumber = contactNumber != null ? contactNumber : "";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AccountRole getRole() {
        return role;
    }

    public void setRole(AccountRole role) {
        this.role = role;
    }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email != null ? email : ""; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName != null ? fullName : ""; }
    
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber != null ? contactNumber : ""; }
} 