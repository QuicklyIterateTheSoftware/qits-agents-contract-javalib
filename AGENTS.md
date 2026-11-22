# qits-agents-contract-javalib — working notes

Read `README.md` first: it is the roster author's contract. These are the rules that bite when you
change the jar.

- **No runtime dependencies, no framework.** Every `<projectslug>-agents-roster-javalib` and
  qits-agents-service depend on this jar; anything it pulls in, they all inherit. The JSON writer in
  `AgentRosterDocument` is hand-written for that reason. `quarkus-bom` is imported in
  `dependencyManagement` only, to align junit.
- **The canonical document is a wire format.** Any byte change to `render`/`renderAgent` changes
  every agent's hash and so restarts every agent on the estate. Keep key order, escaping and the
  omission of absent optionals exactly as `AgentRosterDocumentTest`'s golden string pins them; a
  deliberate shape change bumps `AgentRosterDocument.VERSION`.
- **A new optional field must be omitted when empty**, so existing hashes stay stable.
- **Validation lives in `AgentRosters` only.** `Agent` and its builder never validate, so a
  hand-written `AgentDeclaration` is judged by exactly the same rules. Error messages name the
  project, the agent and the field.
- **The test classpath's services file** lists two rosters with different projects; tests needing
  another set build their own class loader (see `refusesTwoRostersForOneProject`).
- **Packages stay under `eu.wohlben.qits.agents.contract`.**

## Building

Every maven invocation on the shared host goes through the lock, one JVM at a time:

    flock /tmp/qits-maven.lock env JAVA_TOOL_OPTIONS=-Xmx1g ./mvnw -B -ntp -s .qits-maven-settings.xml verify
