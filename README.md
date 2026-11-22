# qits-agents-contract-javalib

`eu.wohlben.qits:qits-agents-contract` — the contract through which a qits project declares its
long-running agents ("pets") as code.

## Why

A project's standing agents — what they are told, how they recover, which model they run — are
part of the project, so they are reviewed and released like the rest of it rather than typed into
a form. Each project keeps them in its own `<projectslug>-agents-roster-javalib`, a jar that
implements this contract. qits-agents-service (epic qits-2518) puts those jars on its class path,
loads the rosters and keeps one agent running per declaration.

This jar is plain Java with no runtime dependencies, so every roster jar can depend on it without
inheriting anything.

## API (`eu.wohlben.qits.agents.contract`)

- `AgentRoster` — the `java.util.ServiceLoader` service a project implements: `project()` (its slug)
  and `agents()` (at least one). List the class in
  `META-INF/services/eu.wohlben.qits.agents.contract.AgentRoster`.
- `AgentDeclaration` — one agent: `name()` (default `developer`), `description()` (one line, 1..200
  characters), `brief()` (standing instructions appended to the harness system prompt),
  `recoveryPrompt()` (the turn given when a new version restarts the agent), optional `model()` and
  `effort()`.
- `Agent` — an immutable record implementation with `Agent.builder()` and `Agent.copyOf(..)`.
- `AgentRosters` — `validate`, `load(ClassLoader)` and `forProject(ClassLoader, project)`. Invalid
  rosters, and two rosters naming one project, throw `InvalidAgentRosterException` naming the
  project, the agent and the field. Run `validate` in the roster jar's own tests.
- `AgentRosterDocument` — the canonical document and its hash.

```java
public final class QitsRoster implements AgentRoster {
    public String project() { return "qits"; }
    public List<AgentDeclaration> agents() {
        return List.of(Agent.builder()
                .description("Keeps the qits estate moving")
                .brief("You are the qits developer agent. ...")
                .recoveryPrompt("You were restarted on a new roster version; pick up where you left off.")
                .build());
    }
}
```

## The canonical document and the hash

`AgentRosterDocument.render(roster)` writes the roster as canonical JSON:
`{"agents":[...],"project":"qits","version":1}` — UTF-8, no insignificant whitespace, object keys
sorted by code point, agents sorted by name, strings escaped minimally per RFC 8259 (`"`, `\`,
`\n`, `\r`, `\t`, other control characters as `\u00xx`; everything else literal). An agent object
carries `brief`, `description`, `effort`?, `model`?, `name`, `recoveryPrompt`; an absent optional is
**omitted**, never `null`.

`AgentRosterDocument.hash(agent)` is the lowercase hex SHA-256 of that agent's object.

**qits-agents-service restarts an agent exactly when its hash changes**, and gives it its
`recoveryPrompt` as the first turn. So editing one agent restarts that agent only, reordering the
roster restarts nothing, and because absent optionals are omitted, a future field an agent leaves
empty does not change its hash. Changing the document's shape is a `VERSION` bump and restarts
everything.

## Building

    ./mvnw -B -ntp verify
