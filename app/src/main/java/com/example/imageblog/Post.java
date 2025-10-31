package com.example.imageblog;

public class Post {
    private String author;
    private String title;
    private String text;
    private String publishedDate;
    private String imageUrl;

    public Post(String author, String title, String text, String publishedDate, String imageUrl) {
        this.author = author;
        this.title = title;
        this.text = text;
        this.publishedDate = publishedDate;
        this.imageUrl = imageUrl;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}

