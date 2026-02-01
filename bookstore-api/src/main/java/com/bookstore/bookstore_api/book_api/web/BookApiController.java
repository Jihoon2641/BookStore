package com.bookstore.bookstore_api.book_api.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bookstore.bookstore_api.book_api.request.BookSearchRequest;
import com.bookstore.bookstore_api.util.response.ApiResponse;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.AllArgsConstructor;
import com.bookstore.bookstore_api.book_api.service.BookSearchService;

@RestController
@AllArgsConstructor
public class BookApiController {

    private final BookSearchService bookSearchService;

    @PostMapping("/api/books/search")
    public ResponseEntity<ApiResponse<String>> searchBooks(@RequestBody BookSearchRequest request) {
        String keyword = request.title();

        int result = bookSearchService.searchBooksAndSave(keyword);

        return ResponseEntity.ok(ApiResponse.success(String.valueOf(result), "성공", HttpStatus.OK));
    }
}
