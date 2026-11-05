package com.iitsm.intelligence;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns free text into a set of meaningful words so two descriptions can be compared:
 * lower-case, punctuation removed, common words ("the", "is", "to", ...) dropped, simple plurals
 * reduced, and synonyms mapped to one word (e.g. "transaction", "checkout" and "pay" all become
 * "payment"; "down", "failing" and "unable" all become "outage").
 */
public class TextNormalizer {

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "be", "been", "being", "to", "of",
            "in", "on", "at", "for", "from", "with", "by", "as", "it", "its", "this", "that", "these", "those",
            "i", "we", "you", "they", "he", "she", "my", "our", "your", "their", "me", "us", "them", "not", "no",
            "do", "does", "did", "can", "could", "will", "would", "should", "have", "has", "had", "make", "get",
            "gets", "getting", "when", "while", "after", "before", "again", "all", "any", "some", "very", "just",
            "still", "now", "since", "users", "user", "customers", "customer", "people", "anyone", "everyone",
            "please", "help", "issue", "problem", "today", "morning");

    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("transaction", "payment"), Map.entry("pay", "payment"), Map.entry("paying", "payment"),
            Map.entry("paid", "payment"), Map.entry("checkout", "payment"), Map.entry("upi", "payment"),
            Map.entry("down", "outage"), Map.entry("unavailable", "outage"), Map.entry("failing", "outage"),
            Map.entry("fail", "outage"), Map.entry("failed", "outage"), Map.entry("failure", "outage"),
            Map.entry("unable", "outage"), Map.entry("cannot", "outage"), Map.entry("cant", "outage"),
            Map.entry("error", "outage"), Map.entry("broken", "outage"), Map.entry("crash", "outage"),
            Map.entry("crashing", "outage"), Map.entry("offline", "outage"), Map.entry("unreachable", "outage"),
            Map.entry("app", "application"), Map.entry("login", "authentication"), Map.entry("signin", "authentication"),
            Map.entry("sso", "authentication"), Map.entry("slow", "performance"), Map.entry("timeout", "performance"),
            Map.entry("db", "database"), Map.entry("wifi", "network"), Map.entry("internet", "network"),
            Map.entry("vpn", "network"), Map.entry("mail", "email"));

    public Set<String> normalize(String text) {
        if (text == null) {
            return Set.of();
        }
        String cleaned = text.toLowerCase(Locale.ROOT)
                .replace("can't", "cant").replace("cannot", "cannot")
                .replace("sign in", "signin").replace("log in", "login")
                .replaceAll("[^a-z0-9 ]", " ");
        return Arrays.stream(cleaned.split("\\s+"))
                .filter(w -> w.length() > 1 && !STOPWORDS.contains(w))
                .map(TextNormalizer::singular)
                .map(w -> SYNONYMS.getOrDefault(w, w))
                .collect(Collectors.toCollection(HashSet::new));
    }

    /** Jaccard similarity: shared words / all distinct words (0 = nothing in common, 1 = identical). */
    public static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    private static String singular(String word) {
        if (word.length() > 4 && word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }
}
