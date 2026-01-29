package com.bookstore.bookstore_api.book_api.response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
public class NaverBookResponse {

    private List<BookItem> items;

    @Getter @Setter
    public static class BookItem {
        private String title;
        private String author;
        private String publisher;
        private String isbn; 
        private Integer discount;
        private String image;    
        private String description;
        private String pubdate;  
    }

}
