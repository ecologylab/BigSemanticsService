package ecologylab.bigsemantics.service.mmdrepository;

import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.log4j.NDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.serialization.formatenums.StringFormat;

/**
 * mmdrepository.jsonp root resource
 * 
 * @author ajit
 */
@Path("/mmdrepository.jsonp")
public class MMDRepositoryJSONPService
{

  static Logger   logger = LoggerFactory.getLogger(MMDRepositoryJSONPService.class);

  static Response resp   = null;

  @GET
  @Produces("application/json")
  public Response getMmdRepository(@QueryParam("callback") String callback)
  {
    NDC.push("mmdrepository | format: json");
    long requestTime = System.currentTimeMillis();
    logger.debug("Requested at: " + (new Date(requestTime)));

    if (resp == null)
      resp = MMDRepositoryServiceHelper.getMmdRepository(StringFormat.JSON);

    String respEntity = callback + "(" + (String) resp.getEntity() + ");";
    Response jsonpResp = Response.status(resp.getStatus()).entity(respEntity).build();

    logger.debug("Time taken (ms): " + (System.currentTimeMillis() - requestTime));
    NDC.remove();

    return jsonpResp;
  }

}
