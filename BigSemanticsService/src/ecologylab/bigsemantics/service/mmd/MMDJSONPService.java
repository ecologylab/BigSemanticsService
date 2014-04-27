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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ecologylab.bigsemantics.service.SemanticsServiceScope;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * mmd.json root resource requests are made with url parameter and are redirected to name parameter
 * 
 * @author ajit
 */
@Path("/mmd.jsonp")
@Component
@Scope("singleton")
public class MMDJSONPService
{

  static Logger                logger       = LoggerFactory.getLogger(MMDJSONPService.class);

  static SemanticsServiceScope serviceScope = SemanticsServiceScope.get();

  // request specific UriInfo object to get absolute query path
  @Context
  UriInfo                      uriInfo;

  @GET
  @Produces("text/plain")
  public Response getMmd(@QueryParam("url") String url,
                         @QueryParam("name") String name,
                         @QueryParam("callback") String callback)
  {
    NDC.push("format: jsonp | url:" + url + " | name:" + name);
    long requestTime = System.currentTimeMillis();
    logger.debug("Requested at: " + (new Date(requestTime)));

    Response resp = MMDServiceHelper.getMmdResponse(url, name, callback, uriInfo, StringFormat.JSON);

    logger.debug("Time taken (ms): " + (System.currentTimeMillis() - requestTime));
    NDC.remove();

    return resp;
  }

}
