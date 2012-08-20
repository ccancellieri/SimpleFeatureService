package it.geosolutions.sfs.client;

import it.geosolutions.sfs.client.utils.Books;
import it.geosolutions.sfs.model.Book;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/test-beans.xml" })
public class RestClientTest {

	@Autowired
	@Qualifier("restTemplate")
	private RestTemplate restTemplate;
	
	@Test
	public void restJsonClientTest() {
		
		Books booksAux = restTemplate.getForObject("http://localhost:8080/sfs/book/names.json", Books.class);
		List<Book> books = booksAux.getBooks();
		
		Assert.assertNotNull(books);
		Assert.assertTrue(books.size() > 0);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void restXmlClientTest() {
		
		List<Book> books = (ArrayList<Book>) restTemplate.getForObject("http://localhost:8080/sfs/book/names.xml", List.class);
		
		Assert.assertNotNull(books);
		Assert.assertTrue(books.size() > 0);
	}
}
