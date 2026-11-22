package eu.wohlben.qits.agents.contract;

import java.util.List;

/** Rosters the tests discover through ServiceLoader. Public with public constructors for it. */
public final class TestRosters {

    private TestRosters() {
    }

    static Agent agent(String name) {
        return Agent.builder().name(name).description(name + " agent")
                .brief("Work on " + name + ".").recoveryPrompt("You were restarted.").build();
    }

    /** Listed in the test classpath's services file. */
    public static final class Alpha implements AgentRoster {
        @Override
        public String project() {
            return "alpha";
        }

        @Override
        public List<AgentDeclaration> agents() {
            return List.of(agent("developer"));
        }
    }

    /** Listed in the test classpath's services file. */
    public static final class Beta implements AgentRoster {
        @Override
        public String project() {
            return "beta";
        }

        @Override
        public List<AgentDeclaration> agents() {
            return List.of(agent("developer"), agent("reviewer"));
        }
    }

    /** Claims project alpha too; only ever listed by a temporary class loader. */
    public static final class AlphaAgain implements AgentRoster {
        @Override
        public String project() {
            return "alpha";
        }

        @Override
        public List<AgentDeclaration> agents() {
            return List.of(agent("other"));
        }
    }
}
