package eu.wohlben.qits.agents.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AgentRosterDocumentTest {

    private static final Agent REVIEWER = Agent.builder().name("reviewer").description("Reviews changes")
            .brief("Review.").recoveryPrompt("Resume review.").model("opus").effort("high").build();
    private static final Agent DEVELOPER = Agent.builder().description("Writes code")
            .brief("Code.").recoveryPrompt("Resume coding.").build();

    private static AgentRoster roster(AgentDeclaration... agents) {
        return new AgentRoster() {
            @Override
            public String project() {
                return "qits";
            }

            @Override
            public List<AgentDeclaration> agents() {
                return List.of(agents);
            }
        };
    }

    @Test
    void rendersCanonicalRoster() {
        assertEquals("{\"agents\":["
                + "{\"brief\":\"Code.\",\"description\":\"Writes code\",\"name\":\"developer\","
                + "\"recoveryPrompt\":\"Resume coding.\"},"
                + "{\"brief\":\"Review.\",\"description\":\"Reviews changes\",\"effort\":\"high\","
                + "\"model\":\"opus\",\"name\":\"reviewer\",\"recoveryPrompt\":\"Resume review.\"}"
                + "],\"project\":\"qits\",\"version\":1}",
                AgentRosterDocument.render(roster(REVIEWER, DEVELOPER)));
    }

    @Test
    void escapesPerRfc8259() {
        Agent agent = Agent.builder().description("d")
                .brief("q\" b\\ n\n t\t c\u0001 r\r ué/").recoveryPrompt("r").build();
        assertEquals("{\"brief\":\"q\\\" b\\\\ n\\n t\\t c\\u0001 r\\r ué/\",\"description\":\"d\","
                + "\"name\":\"developer\",\"recoveryPrompt\":\"r\"}", AgentRosterDocument.renderAgent(agent));
        assertTrue(AgentRosterDocument.renderAgent(Agent.builder().description("d").brief("\u001f")
                .recoveryPrompt("r").build()).contains("\\u001f"));
    }

    @Test
    void hashIsDeterministicLowercaseHex() {
        String hash = AgentRosterDocument.hash(DEVELOPER);
        assertTrue(hash.matches("[0-9a-f]{64}"), hash);
        assertEquals(hash, AgentRosterDocument.hash(Agent.copyOf(DEVELOPER)));
        assertEquals(hash, AgentRosterDocument.hash(roster(REVIEWER, DEVELOPER), "developer"));
    }

    @Test
    void changingOneAgentChangesOnlyItsHash() {
        Agent changed = Agent.builder().name("reviewer").description("Reviews changes")
                .brief("Review harder.").recoveryPrompt("Resume review.").model("opus").effort("high").build();
        AgentRoster before = roster(DEVELOPER, REVIEWER);
        AgentRoster after = roster(DEVELOPER, changed);
        assertEquals(AgentRosterDocument.hash(before, "developer"), AgentRosterDocument.hash(after, "developer"));
        assertNotEquals(AgentRosterDocument.hash(before, "reviewer"), AgentRosterDocument.hash(after, "reviewer"));
    }

    @Test
    void emptyOptionalsHashTheSameAcrossImplementations() {
        AgentDeclaration handWritten = new AgentDeclaration() {
            @Override
            public String description() {
                return "Writes code";
            }

            @Override
            public String brief() {
                return "Code.";
            }

            @Override
            public String recoveryPrompt() {
                return "Resume coding.";
            }
        };
        assertEquals(AgentRosterDocument.renderAgent(DEVELOPER), AgentRosterDocument.renderAgent(handWritten));
        assertEquals(AgentRosterDocument.hash(DEVELOPER), AgentRosterDocument.hash(handWritten));
    }

    @Test
    void hashOfMissingAgentThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> AgentRosterDocument.hash(roster(DEVELOPER), "missing"));
    }
}
