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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.NDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ecologylab.bigsemantics.service.SemanticServiceErrorMessages;
import ecologylab.bigsemantics.service.SemanticsServiceScope;
import ecologylab.net.ParsedURL;
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

  static Logger logger = LoggerFactory.getLogger(MMDJSONPService.class);
  
  static SemanticsServiceScope serviceScope = SemanticsServiceScope.get();

  // request specific UriInfo object to get absolute query path
  @Context
  UriInfo       uriInfo;

  @GET
  @Produces("text/plain")
  public Response getMmd(@QueryParam("url") String url, @QueryParam("name") String name,
                         @QueryParam("callback") String callback)
  {
    NDC.push("format: jsonp | url:" + url + " | name:" + name);
    long requestTime = System.currentTimeMillis();
    logger.debug("Requested at: " + (new Date(requestTime)));

    Response resp = null;
    if (url != null)
    {
      ParsedURL purl = ParsedURL.getAbsolute(url);
      if (purl != null)
        resp = MMDServiceHelper.redirectToMmdByName(purl, uriInfo);
    }
    else if (name != null)
      resp = MMDServiceHelper.getMmdByName(name, StringFormat.JSON);

    // invalid params
    if (resp == null)
      resp = Response.status(Status.BAD_REQUEST).entity(SemanticServiceErrorMessages.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN).build();

    String respEntity = callback + "(" + (String) resp.getEntity() + ");";
    Response jsonpResp = Response.status(resp.getStatus()).entity(respEntity).build();

    logger.debug("Time taken (ms): " + (System.currentTimeMillis() - requestTime));
    NDC.remove();

    return jsonpResp;
  }

}
