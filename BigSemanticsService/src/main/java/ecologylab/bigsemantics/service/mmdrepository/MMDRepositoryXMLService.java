package ecologylab.bigsemantics.service.mmdrepository;

import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.log4j.NDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.service.mmd.MMDJSONPService;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * mmdrepository.xml root resource
 * 
 * @author ajit
 */
@Path("/mmdrepository.xml")
public class MMDRepositoryXMLService
{

  static Logger   logger = LoggerFactory.getLogger(MMDJSONPService.class); ;

  static Response resp   = null;

  @GET
  @Produces("application/xml")
  public Response getMmdRepository()
  {
    NDC.push("mmdrepository | format: xml");
    long requestTime = System.currentTimeMillis();
    logger.debug("Requested at: " + (new Date(requestTime)));

    if (resp == null)
    {
      resp = MMDRepositoryServiceHelper.getMmdRepository(StringFormat.XML);
    }

    logger.debug("Time taken (ms): " + (System.currentTimeMillis() - requestTime));
    NDC.remove();
    return resp;
  }

}
