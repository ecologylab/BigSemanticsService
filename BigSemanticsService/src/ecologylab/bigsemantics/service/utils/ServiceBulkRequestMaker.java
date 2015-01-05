package ecologylab.bigsemantics.service.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import ecologylab.net.ParsedURL;

/**
 * 
 * @author quyin
 * 
 */
public class ServiceBulkRequestMaker extends AbstractBulkRequestMaker
{

  static Logger logger = Logger.getLogger(ServiceBulkRequestMaker.class);

  Client        client;

  public ServiceBulkRequestMaker(String serviceBase)
  {
    super(serviceBase);
    client = new Client();
    client.setFollowRedirects(true);
    client.setConnectTimeout(60000);
  }

  @Override
  public int request(String url)
  {
    String encodedUrl = null;
    try
    {
      encodedUrl = URLEncoder.encode(url, "UTF8");
    }
    catch (UnsupportedEncodingException e1)
    {
      logger.error("Cannot encode URL [" + url + "]");
      e1.printStackTrace();
      return 0;
    }

    String reqUrl = null;
    if (serviceBase.contains("?") && serviceBase.contains("{url}"))
      reqUrl = serviceBase.replace("{url}", encodedUrl);
    else
      reqUrl = String.format("%s?url=%s", serviceBase, encodedUrl);
    String serial = null;
    int status = 0;
    try
    {
      // logger.debug("Accessing " + url + " using request " + reqUrl);
      WebResource resource = client.resource(reqUrl);
      ClientResponse resp = resource == null ? null : resource.get(ClientResponse.class);
      status = resp == null ? 0 : resp.getStatus();
      serial = resp == null ? null : resp.getEntity(String.class);
    }
    catch (Throwable e2)
    {
      logger.error("Network operation failed for [" + reqUrl + "]");
      e2.printStackTrace();
    }

    if (serial != null && serial.length() > 0)
      return status;

    return 0;
  }

}
