package it.geosolutions.sfs.client;

import org.json.simple.JSONArray;
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
		
		JSONArray array = restTemplate.getForObject("http://localhost:8082/sfs/capabilities", JSONArray.class);
		
		Assert.assertNotNull(array);
		Assert.assertTrue(array.size() > 0);
	}
}
