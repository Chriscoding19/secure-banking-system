package banking.account;

/**
 * Thrown when a withdrawal is attempted but the account balance is insufficient.
 */
public class InsufficientFundsException extends Exception {

    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
}
