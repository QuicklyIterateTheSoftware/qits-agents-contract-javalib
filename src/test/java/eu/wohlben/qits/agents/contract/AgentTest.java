package eu.wohlben.qits.agents.contract;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentTest {

    @Test
    void builderDefaultsNameToDeveloper() {
        Agent agent = Agent.builder().description("d").brief("b").recoveryPrompt("r").build();
        assertEquals("developer", agent.name());
        assertEquals(AgentDeclaration.DEFAULT_NAME, agent.name());
        assertEquals(Optional.empty(), agent.model());
        assertEquals(Optional.empty(), agent.effort());
    }

    @Test
    void builderKeepsExplicitValues() {
        Agent agent = Agent.builder().name("reviewer").description("d").brief("b")
                .recoveryPrompt("r").model("opus").effort("high").build();
        assertEquals("reviewer", agent.name());
        assertEquals("d", agent.description());
        assertEquals("b", agent.brief());
        assertEquals("r", agent.recoveryPrompt());
        assertEquals(Optional.of("opus"), agent.model());
        assertEquals(Optional.of("high"), agent.effort());
    }

    @Test
    void buildDoesNotValidateOrThrowOnMissingFields() {
        Agent agent = assertDoesNotThrow(() -> Agent.builder().build());
        assertEquals("developer", agent.name());
        assertNull(agent.description());
    }

    @Test
    void nullOptionalsBecomeEmpty() {
        Agent agent = new Agent("dev", "d", "b", "r", null, null);
        assertEquals(Optional.empty(), agent.model());
        assertEquals(Optional.empty(), agent.effort());
    }

    @Test
    void copyOfSnapshotsAnyDeclaration() {
        String[] brief = {"first"};
        AgentDeclaration live = new AgentDeclaration() {
            @Override
            public String description() {
                return "d";
            }

            @Override
            public String brief() {
                return brief[0];
            }

            @Override
            public String recoveryPrompt() {
                return "r";
            }

            @Override
            public Optional<String> model() {
                return Optional.of("sonnet");
            }
        };
        Agent copy = Agent.copyOf(live);
        brief[0] = "second";
        assertEquals(new Agent("developer", "d", "first", "r", Optional.of("sonnet"), Optional.empty()), copy);
    }

    @Test
    void recordsWithEqualValuesAreEqual() {
        Agent a = Agent.builder().description("d").brief("b").recoveryPrompt("r").build();
        Agent b = new Agent("developer", "d", "b", "r", Optional.empty(), Optional.empty());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
