package org.aksw.qa.systems;

import java.net.URI;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class START extends ASystem {
	Logger log = LoggerFactory.getLogger(START.class);

	public String name() {
		return "start";
	};

	public void search(IQuestion question) throws Exception {
		String questionString;
		if (!question.getLanguageToQuestion().containsKey("en")) {
			return;
		}
		questionString = question.getLanguageToQuestion().get("en");
		log.debug(this.getClass().getSimpleName() + ": " + questionString);

		HttpClient client = HttpClientBuilder.create().build();
		URI uri = new URIBuilder().setScheme("http")
				.setHost("start.csail.mit.edu").setPath("/justanswer.php")
				.setParameter("query", questionString).build();
		HttpGet httpget = new HttpGet(uri);
		HttpResponse response = client.execute(httpget);
		//Test if error occured
		if(response.getStatusLine().getStatusCode()>=400){
			throw new Exception("START Server could not answer due to: "+response.getStatusLine());
		}
		
		Document doc = Jsoup.parse(responseparser.responseToString(response));
		System.out.println(doc.select("span[type=reply]").text());

		// TODO return senseful answer from start
		// return resultSet;
	}
}
