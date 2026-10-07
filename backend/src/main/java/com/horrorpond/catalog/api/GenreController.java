package com.horrorpond.catalog.api;

import com.horrorpond.catalog.domain.Genre;
import com.horrorpond.catalog.repository.GenreRepository;
import com.horrorpond.common.domain.Language;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GenreController {

    private final GenreRepository genreRepository;

    /**
     * @param lang ko(기본) | en
     */
    @GetMapping("/api/genres")
    public List<GenreResponse> genres(
            @RequestParam(defaultValue = "ko") @Pattern(regexp = Language.PARAM_PATTERN) String lang) {
        Language language = Language.from(lang);
        return genreRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(genre -> GenreResponse.from(genre, language))
                .toList();
    }

    public record GenreResponse(String slug, String name, String description) {

        static GenreResponse from(Genre genre, Language language) {
            return new GenreResponse(genre.getSlug(), genre.name(language), genre.description(language));
        }
    }
}
