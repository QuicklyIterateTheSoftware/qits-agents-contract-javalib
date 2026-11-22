package eu.wohlben.qits.agents.contract;

import java.util.Optional;

/**
 * An immutable {@link AgentDeclaration}, the convenient way to write a roster entry. Construction
 * never validates (that is {@link AgentRosters#validate}'s job, so every implementation is judged by
 * the same rules); it only maps a {@code null} model or effort to {@link Optional#empty()}.
 */
public record Agent(
        String name,
        String description,
        String brief,
        String recoveryPrompt,
        Optional<String> model,
        Optional<String> effort) implements AgentDeclaration {

    public Agent {
        model = model == null ? Optional.empty() : model;
        effort = effort == null ? Optional.empty() : effort;
    }

    /** A builder whose name starts at {@link AgentDeclaration#DEFAULT_NAME}. */
    public static Builder builder() {
        return new Builder();
    }

    /** Snapshots any declaration, so later calls on it cannot change what was read. */
    public static Agent copyOf(AgentDeclaration declaration) {
        return new Agent(
                declaration.name(),
                declaration.description(),
                declaration.brief(),
                declaration.recoveryPrompt(),
                declaration.model(),
                declaration.effort());
    }

    /** Mutable builder for {@link Agent}; {@link #build()} never throws. */
    public static final class Builder {
        private String name = DEFAULT_NAME;
        private String description;
        private String brief;
        private String recoveryPrompt;
        private String model;
        private String effort;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder brief(String brief) {
            this.brief = brief;
            return this;
        }

        public Builder recoveryPrompt(String recoveryPrompt) {
            this.recoveryPrompt = recoveryPrompt;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder effort(String effort) {
            this.effort = effort;
            return this;
        }

        public Agent build() {
            return new Agent(name, description, brief, recoveryPrompt,
                    Optional.ofNullable(model), Optional.ofNullable(effort));
        }
    }
}
