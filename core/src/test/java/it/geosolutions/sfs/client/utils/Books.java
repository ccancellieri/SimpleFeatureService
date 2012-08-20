package it.geosolutions.sfs.client.utils;

import it.geosolutions.sfs.model.Book;

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

public class Books {
	
	@JsonProperty("bookList")
	private
	List<Book> books;

	public List<Book> getBooks() {
		return books;
	}

	public void setBooks(List<Book> books) {
		this.books = books;
	}
}
