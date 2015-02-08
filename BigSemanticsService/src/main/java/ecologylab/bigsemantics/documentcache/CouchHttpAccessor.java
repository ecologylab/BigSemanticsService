package ecologylab.bigsemantics.documentcache;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

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

/**
 * This class uses apache's httpclient version 4.2 to interface with a couchdb instance.
 * 
 * @author Zach Brown
 */
public class CouchHttpAccessor implements CouchAccessor
{

  private static Logger logger     = LoggerFactory.getLogger(CouchHttpAccessor.class);

  // Where the database service is
  private String        databaseUrl;

  private HttpClient    httpclient = new DefaultHttpClient();

  public CouchHttpAccessor(String databaseUrl)
  {
    if (!databaseUrl.startsWith("http://"))
    {
      databaseUrl = "http://" + databaseUrl;
    }
    this.databaseUrl = databaseUrl;
  }

  @Override
  public String getDoc(String docId, String tableId)
      throws CouchAccessorException, ParseException, IOException
  {
    String location = databaseUrl + "/" + tableId + "/" + docId;
    logger.info("docId = {}, tableId = {}", docId, tableId);

    HttpGet httpget = new HttpGet(location);
    try
    {
      HttpResponse response = httpclient.execute(httpget);
      int status_code = response.getStatusLine().getStatusCode();
      if (status_code == 200 || status_code == 201 || status_code == 202)
      {
        String result = EntityUtils.toString(response.getEntity(), "UTF-8");
        return result;
      }
      else if (status_code == 404)// There's not a major issue, the document just isn't there
      {
        return null;
      }
      else
      // There was some kind of issue
      {
        String explaination = EntityUtils.toString(response.getEntity(), "UTF-8");
        throw new CouchAccessorException(explaination, status_code, databaseUrl, tableId, docId);
      }
    }
    finally
    {
      httpget.releaseConnection();
    }

  }

  @Override
  public boolean putDoc(String docID, String docContent, String tableID)
      throws ParseException, IOException, CouchAccessorException
  {
    String location = databaseUrl + "/" + tableID + "/" + docID;

    HttpPut httpput = new HttpPut(location);
    try
    {
      StringEntity entity = new StringEntity(docContent, "UTF-8");
      // System.out.println("docContent " + docContent);
      entity.setContentType("application/json");
      int size = docContent.length();
      httpput.setEntity(entity);
      String request = EntityUtils.toString(entity);
      // System.out.println(request);
      HttpResponse response = httpclient.execute(httpput);
      // System.out.println(response);
      int status_code = response.getStatusLine().getStatusCode();

      if (status_code == 200 || status_code == 201)
      {
        return true;
      }
      else if (status_code == 409)
      {
        return false;
      }
      else
      {
        HttpEntity error_entity = response.getEntity();
        String error_msg = EntityUtils.toString(error_entity);
        throw new CouchAccessorException(error_msg, status_code, databaseUrl, tableID, docID);
      }
    }
    finally
    {
      httpput.releaseConnection();
    }
  }

  @Override
  public boolean updateDoc(String docID, String docContent, String tableID)
      throws ClientProtocolException, IOException, CouchAccessorException
  {
    String rev = "";
    String location = databaseUrl + "/" + tableID + "/" + docID;

    HttpHead httphead = new HttpHead(location);

    try
    {
      HttpResponse headResponse = httpclient.execute(httphead);
      int status_code = headResponse.getStatusLine().getStatusCode();
      if (status_code == 404)
        return false; // Can't update because the document doesn't exist

      rev = headResponse.getHeaders("ETag")[0].getValue();
      rev = rev.split("\"")[1];

      // This block adds the revision key to the json object, which couchDB requires inorder to
      // update documents.
      int lastbrace = docContent.lastIndexOf("}");
      docContent = docContent.substring(0, lastbrace);
      // System.out.println("This is the json without the las brace" + json);
      docContent = docContent + " , \"_rev\" : \"" + rev + "\" }";
      // System.out.println("This is the json after adding rev " + json);

      HttpPut httpput = new HttpPut(location);
      StringEntity entity = new StringEntity(docContent, "UTF-8");
      entity.setContentType("application/json");
      httpput.setEntity(entity);
      try
      {
        HttpResponse updateResponse = httpclient.execute(httpput);
        status_code = updateResponse.getStatusLine().getStatusCode();
        if (status_code == 200 || status_code == 201)
        {
          return true;
        }
        else
        {
          HttpEntity error_entity = updateResponse.getEntity();
          String error_msg = EntityUtils.toString(error_entity);
          throw new CouchAccessorException(error_msg, status_code, databaseUrl, tableID, docID);
        }
      }
      finally
      {
        httpput.releaseConnection();
      }
    }
    finally
    {
      httphead.releaseConnection();
    }
  }

  @Override
  public boolean dropDoc(String docID, String tableID)
      throws CouchAccessorException, ClientProtocolException, IOException
  {
    if (docID.trim().equals(""))
    {
      throw new CouchAccessorException("Drop Doc should not be used with an empty id that will delete the database!!",
                                       0,
                                       databaseUrl,
                                       tableID,
                                       docID);
    }

    String rev = "";
    String location = databaseUrl + "/" + tableID + "/" + docID;

    HttpHead httphead = new HttpHead(location);
    try
    {
      HttpResponse headResponse = httpclient.execute(httphead);
      int status_code = headResponse.getStatusLine().getStatusCode();
      if (status_code == 404)
        return false;

      rev = headResponse.getHeaders("ETag")[0].getValue();
      rev = rev.split("\"")[1];
      // System.out.println("The revision code is " + rev );
      location = location + "?rev=" + rev;
      HttpDelete httpdelete = new HttpDelete(location);

      try
      {
        HttpResponse delResponse = httpclient.execute(httpdelete);

        status_code = delResponse.getStatusLine().getStatusCode();

        if (status_code == 200 || status_code == 201)
        {
          return true;
        }
        else
        {
          HttpEntity error_entity = delResponse.getEntity();
          String error_msg = EntityUtils.toString(error_entity);
          throw new CouchAccessorException(error_msg, status_code, databaseUrl, tableID, docID);
        }
      }
      finally
      {
        httpdelete.releaseConnection();
      }

    }
    finally
    {
      httphead.releaseConnection();
    }
  }

  @Override
  public boolean putAttach(String docID,
                           String tableID,
                           String content,
                           String mimeType,
                           String contentTitle)
      throws ClientProtocolException, IOException, CouchAccessorException
  {

    String rev = "";
    String location = databaseUrl + "/" + tableID + "/" + docID;

    HttpHead httphead = new HttpHead(location);

    try
    {
      HttpResponse headResponse = httpclient.execute(httphead);
      // System.out.println("Tried " + httphead );
      int status_code = headResponse.getStatusLine().getStatusCode();
      // System.out.println("Code of response " + status_code );
      if (status_code == 404)
      {
        return false;
      }
      rev = headResponse.getHeaders("ETag")[0].getValue();
      rev = rev.split("\"")[1];
      // This title might cause a problem
      location = location + "/" + contentTitle + "/?rev=";
      location += rev;

      byte[] docContent = content.getBytes(Charset.forName("UTF-8"));
      HttpPut httpput = new HttpPut(location);
      ByteArrayEntity entity = new ByteArrayEntity(docContent);
      entity.setContentType(mimeType);
      httpput.setEntity(entity);
      try
      {
        HttpResponse attachResponse = httpclient.execute(httpput);

        // System.out.println("Tried " + httpput );
        status_code = attachResponse.getStatusLine().getStatusCode();
        // System.out.println("Code of response " + status_code );
        if (status_code == 200 || status_code == 201)
        {
          return true;
        }
        else
        {
          HttpEntity error_entity = attachResponse.getEntity();
          String error_msg = EntityUtils.toString(error_entity);
          throw new CouchAccessorException(error_msg, status_code, databaseUrl, tableID, docID);
        }
      }
      finally
      {
        httpput.releaseConnection();
      }
    }
    finally
    {
      httphead.releaseConnection();
    }
  }

  @Override
  public byte[] getAttach(String docId, String tableId, String title)
  {
    String location = databaseUrl + "/" + tableId + "/" + docId + "/" + title;
    logger.info("docId = {}, tableId = {}", docId, tableId);

    HttpGet httpget = new HttpGet(location);
    try
    {
      HttpResponse response = httpclient.execute(httpget);

      int status_code = response.getStatusLine().getStatusCode();

      if (status_code == 200 || status_code == 201 || status_code == 202)
      {
        byte[] result = EntityUtils.toByteArray(response.getEntity());
        return result;
      }
      else
      {
        return null;
      }
    }
    catch (UnsupportedEncodingException e)
    {
      logger.error("docId = " + docId + ", tableId = " + tableId, e);
    }
    catch (ParseException e)
    {
      logger.error("docId = " + docId + ", tableId = " + tableId, e);
    }
    catch (IOException e)
    {
      logger.error("docId = " + docId + ", tableId = " + tableId, e);
    }
    finally
    {
      httpget.releaseConnection();
    }
    return null;
  }
}
