package com.iitsm.web;

import com.iitsm.intelligence.CategorySuggester;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Small JSON endpoint used by the report form to suggest a category while the employee types
 * (feature F). Requires login like every other page.
 */
@RestController
public class SuggestionApiController {

    private final CategorySuggester suggester;

    public SuggestionApiController(CategorySuggester suggester) {
        this.suggester = suggester;
    }

    @GetMapping("/api/suggest-category")
    public ResponseEntity<Map<String, Object>> suggestCategory(@RequestParam(defaultValue = "") String text) {
        return suggester.suggest(text)
                .<ResponseEntity<Map<String, Object>>>map(c -> ResponseEntity.ok(Map.of("id", c.getId(), "name", c.getName())))
                .orElse(ResponseEntity.noContent().build());
    }
}
