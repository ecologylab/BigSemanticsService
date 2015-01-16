package ecologylab.bigsemantics.documentcache;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class uses apache's httpclient version 4.2 
 * to interface with a couchdb instance.
 * 
 * @author Zach Brown
 * 
 * 
 */
public class HttpCouchInterface implements CouchInterface {
	
	private static Logger logger = LoggerFactory.getLogger(HttpCouchInterface.class);

	private String databaseUrl;//Where is the database 
	private HttpClient httpclient = new DefaultHttpClient();
	
	
	public HttpCouchInterface(String databaseUrl){
		this.databaseUrl = databaseUrl;
	}
	
	@Override
	public String getDoc(String docId ,String tableId) {
		
		String location = "http://" + databaseUrl + "/" + tableId + "/" + docId;
		logger.info("docId = {}, tableId = {}", docId, tableId);

		
		HttpGet httpget = new HttpGet(location);
		try {  
	    HttpResponse response = httpclient.execute(httpget);
	    
	    //System.out.println("Tried " + httpget );
	    int status_code = response.getStatusLine().getStatusCode();
	    //System.out.println("Code of response " + status_code );
	    if( status_code == 200 || status_code == 201 || status_code == 202){
	    	String result = EntityUtils.toString(response.getEntity());
	    	return result;
	    }
	    else{
	    	return null;
	    }
		}
		catch (UnsupportedEncodingException e){
			logger.error("docId = " + docId + ", tableId = " + tableId, e);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally
		{
			httpget.releaseConnection();
		}
		return null;
	}

	@Override
	public int putDoc(String docID , String docContent , String tableId) {
		
		String location = "http://" + databaseUrl + "/" + tableId + "/" + docID;

		HttpPut httpput = new HttpPut(location);
		try {
		StringEntity entity = new StringEntity(docContent , "UTF-8");
		//System.out.println("docContent " + docContent);
		entity.setContentType("application/json");
		int size = docContent.length();
		httpput.setEntity(entity);
		String request = EntityUtils.toString(entity);
		//System.out.println(request);
	    HttpResponse response = httpclient.execute(httpput);
	    //System.out.println(response);
	    int status_code = response.getStatusLine().getStatusCode();
	    
	    if (status_code != 200 && status_code != 201)
	    {
	    	HttpEntity error_entity = response.getEntity();
	    	String error_msg = EntityUtils.toString(error_entity);
	    	logger.error("status line: {}", response.getStatusLine());
	    	logger.error("error details: {}", error_msg);
	    }
	    
	    //System.out.println("Code of response " + status_code );
	    return status_code;
		}
		catch (UnsupportedEncodingException e){
			//e.printStackTrace();
			return 1000;
		} catch (ParseException e) {
			//e.printStackTrace();
			return 1100;
		} catch (IOException e) {
			//e.printStackTrace();
			return 1200;
		}finally{
			httpput.releaseConnection();
		}
	}
	@Override
	public int updateDoc(String docId, String docContent , String tableId )
	{
		
		String rev = "";
		String location = "http://" + databaseUrl + "/" + tableId + "/" + docId;
		
		HttpHead httphead = new HttpHead(location);	

		try {  
			
			HttpResponse headResponse = httpclient.execute(httphead);
		    //System.out.println("Tried " + httphead );
		    int status_code = headResponse.getStatusLine().getStatusCode();	
		    //System.out.println("Code of response " + status_code );
		    if( status_code != 200 )
		    	return status_code;
		    
		    rev = headResponse.getHeaders("ETag")[0].getValue();
		    rev = rev.split("\"")[1];
		   // System.out.println("The revision code is " + rev );
		    
		    
		    //This block adds the revision key to the json object, which couchDB requires inorder to update documents. 
		    int lastbrace = docContent.lastIndexOf("}");
		    docContent = docContent.substring(0, lastbrace);
		    //System.out.println("This is the json without the las brace" + json);
		    docContent = docContent +" , \"_rev\" : \"" + rev + "\" }";
		    //System.out.println("This is the json after adding rev "  + json);
	    
		    HttpPut httpput = new HttpPut(location);
			StringEntity entity = new StringEntity(docContent , "UTF-8");  
			entity.setContentType("application/json");
			httpput.setEntity(entity);
			try{
				HttpResponse delResponse = httpclient.execute(httpput);
		    
				//System.out.println("Tried " + httpput );
				status_code = delResponse.getStatusLine().getStatusCode();
				//System.out.println("Code of response " + status_code );
				return status_code;
			}finally{
				httpput.releaseConnection();
			}
	    
		}
		catch (UnsupportedEncodingException e){
			//e.printStackTrace();
			return 1000;
		} catch (ParseException e) {
			//e.printStackTrace();
			return 1100;
		} catch (IOException e) {
			//e.printStackTrace();
			return 1200;
		}
		finally{
			httphead.releaseConnection();
		}

	}


	@Override
	public int dropDoc(String docId, String tableId) {
		
		if( docId.trim().equals("") ){
			System.out.println("Drop Doc should not be used with an empty id that will delete the database");
			return 1300; 
		}
		
		String rev = "";
		String location = "http://" + databaseUrl + "/" + tableId + "/" + docId;
		
		HttpHead httphead = new HttpHead(location);	
		try {  
			HttpResponse headResponse = httpclient.execute(httphead);
		    //System.out.println("Tried " + httphead );
		    int status_code = headResponse.getStatusLine().getStatusCode();
		    //System.out.println("Code of response " + status_code );
			if( status_code != 200)
				return status_code;
		    
		    rev = headResponse.getHeaders("ETag")[0].getValue();
		    rev = rev.split("\"")[1];
		    //System.out.println("The revision code is " + rev );
		    location = location +"?rev="+rev;
			HttpDelete httpdelete = new HttpDelete(location);
			
			try{
			    HttpResponse delResponse = httpclient.execute(httpdelete);
				
			    status_code = delResponse.getStatusLine().getStatusCode();
			    return status_code;
			
			}finally{
				httpdelete.releaseConnection();
			}
		
		}
		catch (UnsupportedEncodingException e){
			//e.printStackTrace();
			return 1000;
		} catch (ParseException e) {
			//e.printStackTrace();
			return 1100;
		} catch (IOException e) {
			//e.printStackTrace();
			return 1200;
		} finally {
			httphead.releaseConnection();
		}
	}
	
	@Override
	public int putAttach(String docId, String tableId, String content, 
			String mimeType , String contentTitle) {
		
		
		String rev = "";
		String location = "http://" + databaseUrl + "/" + tableId + "/" + docId;
		
		HttpHead httphead = new HttpHead(location);	

		try {  
			
			HttpResponse headResponse = httpclient.execute(httphead);
		    //System.out.println("Tried " + httphead );
		    int status_code = headResponse.getStatusLine().getStatusCode();	
		    //System.out.println("Code of response " + status_code );
		    if( status_code != 200 ){
		    	System.out.println("HERE");
		    	return status_code;
		    }
		    rev = headResponse.getHeaders("ETag")[0].getValue();
		    rev = rev.split("\"")[1];
		    //This title might cause a problem
		    location = location +"/"+contentTitle + "/?rev=";
		    location +=rev;
		    
		    byte[] docContent = content.getBytes(Charset.forName("UTF-8"));
		    HttpPut httpput = new HttpPut(location);
			ByteArrayEntity entity = new ByteArrayEntity(docContent);  
			entity.setContentType(mimeType);
			httpput.setEntity(entity);
			try{
				HttpResponse delResponse = httpclient.execute(httpput);
		    
				//System.out.println("Tried " + httpput );
				status_code = delResponse.getStatusLine().getStatusCode();
				//System.out.println("Code of response " + status_code );
				return status_code;
			}finally{
				httpput.releaseConnection();
			}
	    

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			logger.error("docId = " + docId + ", tableId = " + tableId, e);
			return 500; 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("docId = " + docId + ", tableId = " + tableId, e);
			return 1200;
		}
		finally{
			httphead.releaseConnection();
		}
	}
	
	@Override
	public byte[] getAttach(String docId, String tableId, String title) {
		
		String location = "http://" + databaseUrl + "/" + tableId + "/" + docId + "/" + title;
		logger.info("docId = {}, tableId = {}", docId, tableId);

		
		HttpGet httpget = new HttpGet(location);
		try {  
	    HttpResponse response = httpclient.execute(httpget);
	  
	    int status_code = response.getStatusLine().getStatusCode();

	    if( status_code == 200 || status_code == 201 || status_code == 202){
	    	byte[] result = EntityUtils.toByteArray(response.getEntity());
	    	return result;
	    }
	    else{
	    	return null;
	    }
		}
		catch (UnsupportedEncodingException e){
			logger.error("docId = " + docId + ", tableId = " + tableId, e);
		} catch (ParseException e) {
			logger.error("docId = " + docId + ", tableId = " + tableId, e);
			
		} catch (IOException e) {
			logger.error("docId = " + docId + ", tableId = " + tableId, e);
			
		}finally
		{
			httpget.releaseConnection();
		}
		return null;
	}
	
	public static void main(String argsp[]) throws ClientProtocolException, IOException 
	{
		
		HttpCouchInterface ci = new HttpCouchInterface("ecoarray0:7054");
		
		System.out.println(ci.putAttach("test" , "html" , "Working?" , "text/plain" , "My_example"));
	
		byte[] b = ci.getAttach("test", "html", "My_example");
		System.out.println( new String( b ));

	
	
	}
	




	
	


}