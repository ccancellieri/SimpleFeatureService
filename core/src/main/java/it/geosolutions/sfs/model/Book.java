package it.geosolutions.sfs.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("book")
public class Book {

	private Integer id;
	private String name;
	

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

}
