package eu.wohlben.qits.agents.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AgentRostersTest {

    private static AgentRoster roster(String project, AgentDeclaration... agents) {
        List<AgentDeclaration> list = agents == null ? null : new ArrayList<>(Arrays.asList(agents));
        return new AgentRoster() {
            @Override
            public String project() {
                return project;
            }

            @Override
            public List<AgentDeclaration> agents() {
                return list;
            }
        };
    }

    private static Agent.Builder valid() {
        return Agent.builder().description("Keeps the estate tidy").brief("Be tidy.").recoveryPrompt("Resume.");
    }

    private static InvalidAgentRosterException refuse(UnaryOperator<Agent.Builder> change) {
        return assertThrows(InvalidAgentRosterException.class,
                () -> AgentRosters.validate(roster("qits", change.apply(valid()).build())));
    }

    private static void assertNames(InvalidAgentRosterException e, String... parts) {
        for (String part : parts) {
            assertTrue(e.getMessage().contains(part), () -> "'" + e.getMessage() + "' should name " + part);
        }
    }

    @Test
    void acceptsValidRoster() {
        AgentRoster r = roster("qits", valid().build(),
                valid().name("reviewer").model("opus").effort("high").build());
        assertSame(r, AgentRosters.validate(r));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Qits", "1qits", "qi ts", "q_its"})
    void refusesBadProject(String project) {
        assertNames(assertThrows(InvalidAgentRosterException.class,
                () -> AgentRosters.validate(roster(project, valid().build()))), "project");
    }

    @Test
    void refusesNullAndOverlongProject() {
        assertNames(assertThrows(InvalidAgentRosterException.class,
                () -> AgentRosters.validate(roster(null, valid().build()))), "project");
        assertNames(assertThrows(InvalidAgentRosterException.class,
                () -> AgentRosters.validate(roster("q" + "x".repeat(63), valid().build()))), "project");
        AgentRosters.validate(roster("q" + "x".repeat(62), valid().build()));
    }

    @Test
    void refusesMissingOrEmptyAgents() {
        assertNames(assertThrows(InvalidAgentRosterException.class,
                () -> AgentRosters.validate(roster("qits"))), "qits", "agents");
        assertNames(assertThrows(InvalidAgentRosterException.class,
                () -> AgentRosters.validate(roster("qits", (AgentDeclaration[]) null))), "qits", "agents");
    }

    @Test
    void refusesNullAgent() {
        assertNames(assertThrows(InvalidAgentRosterException.class,
                () -> AgentRosters.validate(roster("qits", valid().build(), null))), "qits", "agents[1]");
    }

    @Test
    void refusesBadNames() {
        assertNames(refuse(b -> b.name("Dev")), "qits", "name", "Dev");
        assertNames(refuse(b -> b.name("1dev")), "name", "1dev");
        assertNames(refuse(b -> b.name("d" + "x".repeat(32))), "name");
        assertNames(refuse(b -> b.name(null)), "name");
        AgentRosters.validate(roster("qits", valid().name("d" + "x".repeat(31)).build()));
    }

    @Test
    void refusesDuplicateNames() {
        assertNames(assertThrows(InvalidAgentRosterException.class,
                () -> AgentRosters.validate(roster("qits", valid().build(), valid().build()))),
                "qits", "developer", "name");
    }

    @Test
    void refusesBadDescriptions() {
        assertNames(refuse(b -> b.description("")), "qits", "developer", "description");
        assertNames(refuse(b -> b.description(null)), "description");
        assertNames(refuse(b -> b.description("x".repeat(201))), "description");
        assertNames(refuse(b -> b.description("two\nlines")), "description");
        assertNames(refuse(b -> b.description("two\rlines")), "description");
        AgentRosters.validate(roster("qits", valid().description("x".repeat(200)).build()));
    }

    @Test
    void refusesBlankBriefAndRecoveryPrompt() {
        assertNames(refuse(b -> b.brief(" \n")), "developer", "brief");
        assertNames(refuse(b -> b.brief(null)), "brief");
        assertNames(refuse(b -> b.recoveryPrompt("")), "developer", "recoveryPrompt");
        assertNames(refuse(b -> b.recoveryPrompt(null)), "recoveryPrompt");
    }

    @Test
    void refusesBlankModelAndEffort() {
        assertNames(refuse(b -> b.model(" ")), "developer", "model");
        assertNames(refuse(b -> b.effort("")), "developer", "effort");
    }

    @Test
    void refusesNullOptional() {
        AgentDeclaration nullModel = new AgentDeclaration() {
            @Override
            public String description() {
                return "d";
            }

            @Override
            public String brief() {
                return "b";
            }

            @Override
            public String recoveryPrompt() {
                return "r";
            }

            @Override
            public Optional<String> model() {
                return null;
            }
        };
        assertNames(assertThrows(InvalidAgentRosterException.class,
                () -> AgentRosters.validate(roster("qits", nullModel))), "developer", "model", "null Optional");
    }

    @Test
    void loadsEveryRosterOnTheClassPath() {
        List<AgentRoster> rosters = AgentRosters.load(getClass().getClassLoader());
        assertEquals(List.of("alpha", "beta"), rosters.stream().map(AgentRoster::project).sorted().toList());
    }

    @Test
    void findsOneProject() {
        ClassLoader cl = getClass().getClassLoader();
        assertEquals(2, AgentRosters.forProject(cl, "beta").orElseThrow().agents().size());
        assertEquals(Optional.empty(), AgentRosters.forProject(cl, "gamma"));
    }

    @Test
    void refusesTwoRostersForOneProject(@TempDir Path dir) throws IOException {
        Path services = dir.resolve("META-INF/services");
        Files.createDirectories(services);
        Files.writeString(services.resolve(AgentRoster.class.getName()),
                TestRosters.AlphaAgain.class.getName() + "\n");
        // Parent = the test class loader, which already lists Alpha and Beta.
        try (URLClassLoader cl = new URLClassLoader(new URL[] {dir.toUri().toURL()}, getClass().getClassLoader())) {
            assertNames(assertThrows(InvalidAgentRosterException.class, () -> AgentRosters.load(cl)),
                    "alpha", "two rosters", "AlphaAgain");
        }
    }
}
