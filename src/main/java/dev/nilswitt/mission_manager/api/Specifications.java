package dev.nilswitt.mission_manager.api;

import org.springframework.data.jpa.domain.Specification;

/**
 * Combines optional filter {@link Specification}s, skipping any that are {@code null} (the
 * convention used across list endpoints for filters that weren't supplied by the caller).
 * Spring Data's own {@code Specification.where(...)} rejects a null argument, so plain
 * {@code .and(...)} chaining isn't safe when the first filter in the chain may be absent.
 */
public final class Specifications {

    private Specifications() {}

    @SafeVarargs
    public static <T> Specification<T> allOf(Specification<T>... specs) {
        Specification<T> combined = null;
        for (Specification<T> spec : specs) {
            if (spec == null) {
                continue;
            }
            combined = combined == null ? spec : combined.and(spec);
        }
        return combined;
    }
}
