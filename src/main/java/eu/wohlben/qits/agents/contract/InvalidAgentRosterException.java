package eu.wohlben.qits.agents.contract;

/**
 * A roster that breaks a rule of {@link AgentRosters#validate}. The message names the project, the
 * agent where one is involved, and the field, so the author can fix the declaration without a
 * debugger.
 */
public final class InvalidAgentRosterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidAgentRosterException(String message) {
        super(message);
    }
}
