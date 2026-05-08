import org.mindrot.jbcrypt.BCrypt;

public class HashGen {
    public static void main(String[] args) {
        String pw = "password123";
        String hashed = BCrypt.hashpw(pw, BCrypt.gensalt(13)).replace("$2a$", "$2y$");
        System.out.println(hashed);
    }
}
