package greencloud.impl.scripting.sandbox;

public final class SandboxViolation extends RuntimeException {

    public SandboxViolation(String message) {
        super(message);
    }
}
