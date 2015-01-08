/**
 * 
 */
package ecologylab.bigsemantics.service.mmd;

import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.NDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.serialization.formatenums.StringFormat;

/**
 * mmd.json root resource requests are made with url parameter and are redirected to name parameter
 * 
 * @author ajit
 */
@Path("/mmd.json")
public class MMDJSONService implements MMDServiceParamNames
{
  
  static Logger logger = LoggerFactory.getLogger(MMDJSONService.class);

  // request specific UriInfo object to get absolute query path
  @Context
  UriInfo       uriInfo;

  @GET
  @Produces("application/json")
  public Response getMmd(@QueryParam(URL) String url,
                         @QueryParam(NAME) String name)
  {
    NDC.push("format: json | url:" + url + " | name:" + name);
    long requestTime = System.currentTimeMillis();
    logger.debug("Requested at: " + (new Date(requestTime)));

    Response resp = MMDServiceHelper.getMmdResponse(url,
                                                    name,
                                                    null,
                                                    null,
                                                    uriInfo,
                                                    StringFormat.JSON);
 
    logger.debug("Time taken (ms): " + (System.currentTimeMillis() - requestTime));

    NDC.remove();
    return resp;
  }
  
}
