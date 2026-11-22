package eu.wohlben.qits.agents.contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validation and discovery of {@link AgentRoster}s. Both live here, beside the contract, so a
 * roster's own tests and qits-agents-service refuse exactly the same declarations.
 */
public final class AgentRosters {

    /** An agent name: lowercase, starts with a letter, at most 32 characters. */
    public static final Pattern NAME = Pattern.compile("[a-z][a-z0-9-]{0,31}");

    /** A project slug: lowercase, starts with a letter, at most 63 characters. */
    public static final Pattern PROJECT = Pattern.compile("[a-z][a-z0-9-]{0,62}");

    static final int DESCRIPTION_MAX = 200;

    private AgentRosters() {
    }

    /**
     * Checks every rule of the contract and returns the roster unchanged.
     *
     * @throws InvalidAgentRosterException naming the first rule broken
     */
    public static AgentRoster validate(AgentRoster roster) {
        if (roster == null) {
            throw new InvalidAgentRosterException("roster is null");
        }
        String project = roster.project();
        if (project == null || !PROJECT.matcher(project).matches()) {
            throw new InvalidAgentRosterException("roster of " + roster.getClass().getName()
                    + ": field project " + quote(project) + " must match " + PROJECT.pattern());
        }
        String where = "project " + project;
        List<AgentDeclaration> agents = roster.agents();
        if (agents == null || agents.isEmpty()) {
            throw new InvalidAgentRosterException(where + ": field agents must declare at least one agent");
        }
        Set<String> names = new HashSet<>();
        for (int i = 0; i < agents.size(); i++) {
            AgentDeclaration agent = agents.get(i);
            if (agent == null) {
                throw new InvalidAgentRosterException(where + ": field agents[" + i + "] is null");
            }
            String name = agent.name();
            if (name == null || !NAME.matcher(name).matches()) {
                throw new InvalidAgentRosterException(where + ", agents[" + i + "]: field name "
                        + quote(name) + " must match " + NAME.pattern());
            }
            if (!names.add(name)) {
                throw new InvalidAgentRosterException(where + ", agent " + name
                        + ": field name is declared twice");
            }
            validateAgent(where + ", agent " + name, agent);
        }
        return roster;
    }

    private static void validateAgent(String where, AgentDeclaration agent) {
        String description = agent.description();
        if (description == null || description.isEmpty() || description.length() > DESCRIPTION_MAX) {
            throw new InvalidAgentRosterException(where + ": field description must be 1.."
                    + DESCRIPTION_MAX + " characters");
        }
        if (description.indexOf('\n') >= 0 || description.indexOf('\r') >= 0) {
            throw new InvalidAgentRosterException(where + ": field description must be one line");
        }
        requireText(where, "brief", agent.brief());
        requireText(where, "recoveryPrompt", agent.recoveryPrompt());
        requireOptionalText(where, "model", agent.model());
        requireOptionalText(where, "effort", agent.effort());
    }

    private static void requireText(String where, String field, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidAgentRosterException(where + ": field " + field + " must not be blank");
        }
    }

    private static void requireOptionalText(String where, String field, Optional<String> value) {
        if (value == null) {
            throw new InvalidAgentRosterException(where + ": field " + field
                    + " is a null Optional; return Optional.empty() instead");
        }
        if (value.isPresent() && value.get().isBlank()) {
            throw new InvalidAgentRosterException(where + ": field " + field
                    + " must not be blank when present");
        }
    }

    /**
     * Every roster the class loader provides, each validated.
     *
     * @throws InvalidAgentRosterException when a roster is invalid or two name the same project
     */
    public static List<AgentRoster> load(ClassLoader classLoader) {
        List<AgentRoster> rosters = new ArrayList<>();
        Map<String, AgentRoster> byProject = new HashMap<>();
        for (AgentRoster roster : ServiceLoader.load(AgentRoster.class, classLoader)) {
            validate(roster);
            AgentRoster previous = byProject.putIfAbsent(roster.project(), roster);
            if (previous != null) {
                throw new InvalidAgentRosterException("project " + roster.project()
                        + ": field project is declared by two rosters, "
                        + previous.getClass().getName() + " and " + roster.getClass().getName());
            }
            rosters.add(roster);
        }
        return List.copyOf(rosters);
    }

    /** The validated roster of one project, if the class loader provides it. */
    public static Optional<AgentRoster> forProject(ClassLoader classLoader, String project) {
        return load(classLoader).stream().filter(r -> r.project().equals(project)).findFirst();
    }

    private static String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
