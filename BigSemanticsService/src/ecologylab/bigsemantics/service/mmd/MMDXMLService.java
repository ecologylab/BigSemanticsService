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

import ecologylab.serialization.formatenums.StringFormat;

/**
 * mmd.xml root resource requests are made with url parameter and are redirected to name parameter
 * 
 * @author ajit
 */
@Path("/mmd.xml")
@Component
@Scope("singleton")
public class MMDXMLService implements MMDServiceParamNames
{

  static Logger logger = LoggerFactory.getLogger(MMDXMLService.class);

  // request specific UriInfo object to get absolute query path
  @Context
  UriInfo       uriInfo;

  @GET
  @Produces("application/xml")
  public Response getMmd(@QueryParam(URL) String url,
                         @QueryParam(NAME) String name)
  {
    NDC.push("format: xml | url:" + url + " | name:" + name);
    long requestTime = System.currentTimeMillis();
    logger.debug("Requested at: " + (new Date(requestTime)));

    Response resp = MMDServiceHelper.getMmdResponse(url,
                                                    name,
                                                    null,
                                                    null,
                                                    uriInfo,
                                                    StringFormat.XML);

    logger.debug("Time taken (ms): " + (System.currentTimeMillis() - requestTime));

    NDC.remove();
    return resp;
  }

}
