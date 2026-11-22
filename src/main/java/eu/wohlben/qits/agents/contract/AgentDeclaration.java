package eu.wohlben.qits.agents.contract;

import java.util.Optional;

/**
 * One long-running agent ("pet") a project declares as code. qits-agents-service keeps one agent
 * running per declaration and restarts it whenever its canonical document changes, see
 * {@link AgentRosterDocument#hash(AgentDeclaration)}.
 *
 * <p>Implement it directly or build an {@link Agent}; either way {@link AgentRosters#validate}
 * decides whether the values are acceptable, not the implementation.
 */
public interface AgentDeclaration {

    /** The name an agent gets when its declaration does not choose one. */
    String DEFAULT_NAME = "developer";

    /** Unique within the roster; matches {@link AgentRosters#NAME}. */
    default String name() {
        return DEFAULT_NAME;
    }

    /** One line shown wherever the agent is listed: 1..200 characters, no line break. */
    String description();

    /** Standing instructions, appended to the harness system prompt. Never blank. */
    String brief();

    /** The turn the agent is given when a new version of its declaration restarts it. Never blank. */
    String recoveryPrompt();

    /** The model to run; empty leaves the choice to the platform. */
    default Optional<String> model() {
        return Optional.empty();
    }

    /** The reasoning effort to run at; empty leaves the choice to the platform. */
    default Optional<String> effort() {
        return Optional.empty();
    }
}
