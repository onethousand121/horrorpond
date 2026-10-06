package com.horrorpond.catalog.api;

import com.horrorpond.catalog.domain.Genre;
import com.horrorpond.catalog.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GenreController {

    private final GenreRepository genreRepository;

    @GetMapping("/api/genres")
    public List<GenreResponse> genres() {
        return genreRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(GenreResponse::from)
                .toList();
    }

    public record GenreResponse(String slug, String name, String description) {

        static GenreResponse from(Genre genre) {
            return new GenreResponse(genre.getSlug(), genre.getName(), genre.getDescription());
        }
    }
}
