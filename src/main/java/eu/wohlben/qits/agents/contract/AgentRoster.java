package eu.wohlben.qits.agents.contract;

import java.util.List;

/**
 * The {@link java.util.ServiceLoader} service a project implements in its own
 * {@code <projectslug>-agents-roster-javalib}, listed in
 * {@code META-INF/services/eu.wohlben.qits.agents.contract.AgentRoster}. A ServiceLoader rather than
 * a config file, so the roster is compiled, reviewed and released like any other code.
 */
public interface AgentRoster {

    /** The slug of the project these agents belong to, e.g. {@code "qits"}. */
    String project();

    /** The agents the project keeps running; at least one. */
    List<AgentDeclaration> agents();
}
