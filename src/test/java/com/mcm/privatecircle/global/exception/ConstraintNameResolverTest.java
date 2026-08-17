package com.mcm.privatecircle.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConstraintNameResolverTest {

    @Test
    void 중첩된_예외에서_알려진_제약명을_찾는다() {
        RuntimeException exception = new RuntimeException(
            "outer",
            new IllegalStateException("Duplicate entry for key ''uk_visit_records_visit''")
        );

        assertThat(ConstraintNameResolver.contains(exception, "uk_visit_records_visit")).isTrue();
        assertThat(ConstraintNameResolver.contains(exception, "unknown_constraint")).isFalse();
    }

    @Test
    void null과_순환_cause를_안전하게_처리한다() {
        assertThat(ConstraintNameResolver.contains(null, "uk_visit_records_visit")).isFalse();
        assertThat(ConstraintNameResolver.contains(new RuntimeException(), null)).isFalse();
    }
}
