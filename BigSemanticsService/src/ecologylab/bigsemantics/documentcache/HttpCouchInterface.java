package ecologylab.bigsemantics.documentcache;

import java.io.IOException;


import java.io.UnsupportedEncodingException;

import java.util.Scanner;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;


import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;

import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/*
 * Author Zach Brown
 * This class uses apache's httpclient version 4.2 
 * to interface with a couchdb instance.
 * 
 */
public class HttpCouchInterface implements CouchInterface {

	private String database_url;//Where is the database 
	
	public HttpCouchInterface(String database_url){
		this.database_url = database_url;
	}
	
	@Override
	public String getDoc(String id ,String dbid) {
		
		HttpClient httpclient = new DefaultHttpClient();
		String location = "http://" + database_url + "/" + dbid + "/" + id;

		
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
	    	return "";
	    }
		}
		catch (UnsupportedEncodingException e){
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public int putDoc(String id , String json , String dbid) {
		
		HttpClient httpclient = new DefaultHttpClient();
		String location = "http://" + database_url + "/" + dbid + "/" + id;

		HttpPut httpput = new HttpPut(location);
		try {
		StringEntity entity = new StringEntity(json);  
		entity.setContentType("application/json");
		httpput.setEntity(entity);
	    HttpResponse response = httpclient.execute(httpput);
	    
	    //What should I do with this?
	    //System.out.println("Tried " + httpput );
	    int status_code = response.getStatusLine().getStatusCode();
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
		}
	}

	@Override
	public int updateDoc(String id, String json , String dbid) {
		
		
		HttpClient httpclient = new DefaultHttpClient();
		
		String rev = "";
		String location = "http://" + database_url + "/" + dbid + "/" + id;
		
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
	    int lastbrace = json.lastIndexOf("}");
	    json = json.substring(0, lastbrace);
	    //System.out.println("This is the json without the las brace" + json);
	    json = json +" , \"_rev\" : \"" + rev + "\" }";
	    //System.out.println("This is the json after adding rev "  + json);
	    
	    HttpPut httpput = new HttpPut(location);
		StringEntity entity = new StringEntity(json);  
		entity.setContentType("application/json");
		httpput.setEntity(entity);
	    HttpResponse delResponse = httpclient.execute(httpput);
	    
	    //System.out.println("Tried " + httpput );
	    status_code = delResponse.getStatusLine().getStatusCode();
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
		}

	}
	

	@Override
	public int dropDoc(String id, String dbid) {
		
		HttpClient httpclient = new DefaultHttpClient();
	
		if( id.trim().equals("") ){
			System.out.println("Drop Doc should not be used with an empty id that will delete the database");
			return 1300; 
		}
		
		
		
		
		String rev = "";
		String location = "http://" + database_url + "/" + dbid + "/" + id;
		
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
	    HttpResponse delResponse = httpclient.execute(httpdelete);
	    
	    //System.out.println("Tried " + httpdelete );
	    status_code = delResponse.getStatusLine().getStatusCode();
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
		}
	}
	
	public static void main(String argsp[]) throws ClientProtocolException, IOException 
	{
		
		HttpCouchInterface ci = new HttpCouchInterface("ecoarray0:2084");
		/*
		ci.putDoc("new21" , "{\"f\" : 1 }" , "html_database");
		String result = ci.getDoc("new21" ,  "html_database");
		ci.dropDoc("new21" , "html_database");
		ci.putDoc("new21" , "{\"f\" : 1 }" , "html_database");
		ci.updateDoc("new21", "{\"f\" : 2 }" , "html_database");
		*/
		System.out.println("Adding 100 document");
		for(int i = 0; i < 100; i++)
		{
			int code = ci.putDoc("new" + i , "{\"f\" : " +  i + " }" , "test_database");
			
		}
		
		System.out.println("Press <enter> to update all 100 documents");
		Scanner input = new Scanner(System.in);
		input.nextLine();
		System.out.println("Updating 100 document");
		for(int i =0; i < 100; i++)
		{
			int code = ci.updateDoc("new" + i,"{\"f\" : " +  -i + " }" , "test_database");
			
		}
		
		System.out.println("Press <enter> to get all 100 documents");
		input.nextLine();
		System.out.println("Getting 100 document");
		for(int i =0; i < 100; i++)
		{
			String doc = ci.getDoc("new" + i, "test_database");
			System.out.println(doc);
		}
		
		System.out.println("Press <enter> to delete all 100 documents");
		input.nextLine();
		System.out.println("Deleting 100 document");
		for(int i=0; i < 100; i++)
		{
			int code = ci.dropDoc("new" + i, "test_database");
		}

	}
	


}
