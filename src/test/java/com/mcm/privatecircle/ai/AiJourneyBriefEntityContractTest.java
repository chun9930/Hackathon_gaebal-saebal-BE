package com.mcm.privatecircle.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.junit.jupiter.api.Test;

class AiJourneyBriefEntityContractTest {

    @Test
    void AI_브리프_Entity는_ERD의_테이블과_필수_컬럼_계약을_따른다() throws Exception {
        Class<?> entityType = Class.forName(
            "com.mcm.privatecircle.ai.entity.AiJourneyBrief"
        );

        Table table = entityType.getAnnotation(Table.class);
        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo("ai_journey_briefs");

        assertJoinColumn(entityType, "customer", "customer_id", false);
        assertJoinColumn(entityType, "visit", "visit_id", false);
        assertJoinColumn(entityType, "requestedByCa", "requested_by_ca_id", false);
        assertColumn(entityType, "sourceVisitCount", "source_visit_count", false);
        assertColumn(entityType, "status", "status", false);
        assertColumn(entityType, "generatedAt", "generated_at", false);

        for (String nullableSummary : Arrays.asList(
            "summary",
            "visitPurposeSummary",
            "interestSummary",
            "cautionSummary",
            "suggestedDirection"
        )) {
            assertThat(entityType.getDeclaredField(nullableSummary).getAnnotation(Column.class))
                .isNotNull();
        }
    }

    @Test
    void 브리프_상태는_GENERATED와_FAILED만_허용한다() throws Exception {
        Class<?> enumType = Class.forName("com.mcm.privatecircle.ai.entity.BriefStatus");

        assertThat(enumType.getEnumConstants())
            .extracting(Object::toString)
            .containsExactly("GENERATED", "FAILED");
    }

    private void assertJoinColumn(
        Class<?> entityType,
        String fieldName,
        String columnName,
        boolean nullable
    ) throws Exception {
        Field field = entityType.getDeclaredField(fieldName);
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        assertThat(joinColumn).isNotNull();
        assertThat(joinColumn.name()).isEqualTo(columnName);
        assertThat(joinColumn.nullable()).isEqualTo(nullable);
    }

    private void assertColumn(
        Class<?> entityType,
        String fieldName,
        String columnName,
        boolean nullable
    ) throws Exception {
        Field field = entityType.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
    }
}
