package com.iitsm.intelligence;

import com.iitsm.domain.catalog.Category;
import com.iitsm.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Intelligent feature F - suggests a category from the incident description by counting how many of
 * each category's keywords (normalised the same way as duplicate detection) appear in the text.
 * Administrators control the keywords on the Categories page, so suggestions improve without code.
 */
@Service
@Transactional(readOnly = true)
public class CategorySuggester {

    private final CategoryRepository categories;
    private final TextNormalizer normalizer = new TextNormalizer();

    public CategorySuggester(CategoryRepository categories) {
        this.categories = categories;
    }

    public Optional<Category> suggest(String text) {
        Set<String> words = normalizer.normalize(text);
        if (words.isEmpty()) {
            return Optional.empty();
        }
        return categories.findByActiveTrueOrderByNameAsc().stream()
                .map(c -> new Scored(c, matches(words, c)))
                .filter(s -> s.score > 0)
                .max(Comparator.comparingInt((Scored s) -> s.score)
                        .thenComparing(s -> s.category.getName(), Comparator.reverseOrder()))
                .map(s -> s.category);
    }

    private int matches(Set<String> words, Category category) {
        Set<String> keywords = new HashSet<>(normalizer.normalize(String.join(" ", category.keywordList())));
        keywords.addAll(normalizer.normalize(category.getName()));
        keywords.retainAll(words);
        return keywords.size();
    }

    private record Scored(Category category, int score) {
    }
}
