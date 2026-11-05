package com.iitsm.intelligence;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TextSimilarityTest {

    private final TextNormalizer normalizer = new TextNormalizer();

    @Test
    void normalize_dropsStopwordsAndMapsSynonyms() {
        assertThat(normalizer.normalize("The payment application is unavailable!"))
                .containsExactlyInAnyOrder("payment", "application", "outage");
        assertThat(normalizer.normalize("Payment transaction failing"))
                .containsExactlyInAnyOrder("payment", "outage");
        assertThat(normalizer.normalize("Unable to make payment"))
                .containsExactlyInAnyOrder("outage", "payment");
        assertThat(normalizer.normalize("Can't log in to SSO")).containsExactlyInAnyOrder("outage", "authentication");
    }

    @Test
    void jaccard_examplesFromTheBrief() {
        Set<String> a = normalizer.normalize("Payment application unavailable");
        Set<String> b = normalizer.normalize("Payment transaction failing");
        assertThat(TextNormalizer.jaccard(a, b)).isCloseTo(0.667, org.assertj.core.data.Offset.offset(0.01));
        assertThat(TextNormalizer.jaccard(a, normalizer.normalize("VPN keeps disconnecting"))).isZero();
        assertThat(TextNormalizer.jaccard(Set.of(), b)).isZero();
    }
}
