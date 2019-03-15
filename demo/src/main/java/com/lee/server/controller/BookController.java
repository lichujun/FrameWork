package com.lee.server.controller;

import com.lee.http.annotation.RequestMapping;
import com.lee.iocaop.annotation.Controller;
import com.lee.server.common.CommonResponse;
import com.lee.server.entity.Author;
import com.lee.server.entity.Book;

import java.util.Optional;

/**
 * @author lichujun
 * @date 2019/2/17 10:40 AM
 */
//@Controller
public class BookController {

    @RequestMapping("/book")
    public CommonResponse<Book> setAuthor(Book book, Author author) {
        Optional.ofNullable(book)
                .ifPresent(it -> it.setAuthor(author));
        return CommonResponse.buildOkRes(book);
    }
}
