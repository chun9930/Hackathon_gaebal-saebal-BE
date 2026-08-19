package com.mcm.privatecircle.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class AiBriefSourceContractTest {

    @Test
    void AI_Source는_허용된_데이터_묶음만_노출한다() throws Exception {
        assertRecordComponents(
            "com.mcm.privatecircle.ai.dto.AiBriefSource",
            "customer",
            "currentVisitRecord",
            "visitRecords",
            "interestProducts",
            "purchases",
            "sourceVisitCount"
        );
    }

    @Test
    void 고객_Source에는_개인정보가_아닌_공통_프로필만_포함한다() throws Exception {
        assertRecordComponents(
            "com.mcm.privatecircle.ai.dto.AiBriefSource$CustomerProfile",
            "membershipGrade",
            "stylePreferences"
        );
    }

    @Test
    void 이력_Source는_확정된_필드만_포함한다() throws Exception {
        assertRecordComponents(
            "com.mcm.privatecircle.ai.dto.AiBriefSource$VisitRecordSource",
            "visitedAt",
            "visitPurpose",
            "content",
            "styleChangeNote",
            "cautionNote"
        );
        assertRecordComponents(
            "com.mcm.privatecircle.ai.dto.AiBriefSource$InterestProductSource",
            "productName",
            "category",
            "sourceType",
            "memo",
            "savedAt"
        );
        assertRecordComponents(
            "com.mcm.privatecircle.ai.dto.AiBriefSource$PurchaseSource",
            "productName",
            "category",
            "quantity",
            "purchasedAt"
        );
    }

    private void assertRecordComponents(String className, String... expected) throws Exception {
        Class<?> type = Class.forName(className);
        assertThat(type.isRecord()).isTrue();
        assertThat(Arrays.stream(type.getRecordComponents())
            .map(RecordComponent::getName))
            .containsExactly(expected);
    }
}
