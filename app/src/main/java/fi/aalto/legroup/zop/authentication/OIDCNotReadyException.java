package fi.aalto.legroup.zop.authentication;

public class OIDCNotReadyException extends Exception {
    public OIDCNotReadyException() {
        super("OpenID Connect is not ready");
    }
}
