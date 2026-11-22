package eu.wohlben.qits.agents.contract;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * The canonical JSON form of a roster and of each agent in it, and the hash over it.
 *
 * <p>qits-agents-service restarts an agent exactly when its hash changes, so the document must be
 * byte-stable: no insignificant whitespace, object keys sorted by code point, agents sorted by name,
 * strings escaped minimally per RFC 8259. Absent optionals are omitted rather than written as
 * {@code null}, which keeps every existing hash stable when the contract grows a field an agent
 * leaves empty. Hand-written so the contract jar carries no dependency.
 */
public final class AgentRosterDocument {

    /** The document's format version, written at the top level. */
    public static final int VERSION = 1;

    private static final Comparator<AgentDeclaration> BY_NAME = Comparator.comparing(AgentDeclaration::name);

    private AgentRosterDocument() {
    }

    /** {@code {"agents":[...],"project":"...","version":1}}, agents sorted by name. */
    public static String render(AgentRoster roster) {
        List<AgentDeclaration> agents = new ArrayList<>(roster.agents());
        agents.sort(BY_NAME);
        StringBuilder out = new StringBuilder("{\"agents\":[");
        for (int i = 0; i < agents.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            writeAgent(out, agents.get(i));
        }
        out.append("],\"project\":");
        writeString(out, roster.project());
        out.append(",\"version\":").append(VERSION).append('}');
        return out.toString();
    }

    /** One agent object: brief, description, effort?, model?, name, recoveryPrompt. */
    public static String renderAgent(AgentDeclaration agent) {
        StringBuilder out = new StringBuilder();
        writeAgent(out, agent);
        return out.toString();
    }

    /**
     * The hash of the named agent of the roster.
     *
     * @throws IllegalArgumentException when the roster declares no such agent
     */
    public static String hash(AgentRoster roster, String agentName) {
        return roster.agents().stream()
                .filter(a -> a.name().equals(agentName))
                .findFirst()
                .map(AgentRosterDocument::hash)
                .orElseThrow(() -> new IllegalArgumentException(
                        "project " + roster.project() + " declares no agent " + agentName));
    }

    /** Lowercase hex SHA-256 over the UTF-8 bytes of {@link #renderAgent}. */
    public static String hash(AgentDeclaration agent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(renderAgent(agent).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required of every JVM", e);
        }
    }

    private static void writeAgent(StringBuilder out, AgentDeclaration agent) {
        out.append("{\"brief\":");
        writeString(out, agent.brief());
        out.append(",\"description\":");
        writeString(out, agent.description());
        writeOptional(out, "effort", agent.effort());
        writeOptional(out, "model", agent.model());
        out.append(",\"name\":");
        writeString(out, agent.name());
        out.append(",\"recoveryPrompt\":");
        writeString(out, agent.recoveryPrompt());
        out.append('}');
    }

    private static void writeOptional(StringBuilder out, String key, Optional<String> value) {
        if (value != null && value.isPresent()) {
            out.append(",\"").append(key).append("\":");
            writeString(out, value.get());
        }
    }

    private static void writeString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
