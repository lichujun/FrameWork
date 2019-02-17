package com.lee.server.entity;

import lombok.Data;

/**
 * @author lichujun
 * @date 2019/2/17 10:40 AM
 */
@Data
public class Book {

    private String bookName;
    private String category;
    private Author author;
}
