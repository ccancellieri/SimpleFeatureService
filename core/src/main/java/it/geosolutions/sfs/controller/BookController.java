package it.geosolutions.sfs.controller;

import it.geosolutions.sfs.model.Book;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;


@Controller
@RequestMapping("/book")
public class BookController {

	@RequestMapping(value="/names", method=RequestMethod.GET)
	public List<Book> getNames(@RequestParam(value="id",required=false) Integer id) {
		
		return returnData(null);
	}
	
	@RequestMapping(value="/name/{id}", method=RequestMethod.GET)
        public List<Book> getName(@PathVariable(value="id") Integer id) {
                
                return returnData(id);
        }
	
	private List<Book> returnData(Integer id) {
		
		ArrayList<Book> names = new ArrayList<Book>();
		
		Book b1 = new Book();
		Book b2 = new Book();
		Book b3 = new Book();
		
		b1.setName("book nro. 1");
		b1.setId(0);
		b2.setName("book nro. 2");
		b2.setId(1);
		b3.setName("book nro. 3");
		b3.setId(2);
		
		names.add(b1);
		names.add(b2);
		names.add(b3);
		
		Book b4 = new Book();
                b4.setName("book nro. "+id);
                b4.setId(id);
                names.add(b4);
		
		return names;
	}
}
