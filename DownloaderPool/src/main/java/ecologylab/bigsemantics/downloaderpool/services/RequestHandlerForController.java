package ecologylab.bigsemantics.downloaderpool.services;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.downloaderpool.Controller;
import ecologylab.bigsemantics.downloaderpool.DPoolUtils;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * A base class for request handlers on the controller side.
 * 
 * @author quyin
 */
public class RequestHandlerForController
{

  protected Logger   logger;

  private Controller controller;

  public RequestHandlerForController()
  {
    logger = LoggerFactory.getLogger(this.getClass());
    logger.info(this.getClass().getName() + " Constructed");
  }

  public Controller getController()
  {
    return controller;
  }

  public void setController(Controller controller)
  {
    this.controller = controller;
    controller.start();
    logger.info("Controller hooked and started (if not yet).");
  }

  protected Response generateResponse(Object result,
                                      StringFormat serialFormat,
                                      String mediaType,
                                      String errorMsg)
  {
    String content = DPoolUtils.serialize(result, serialFormat);
    Response resp = content == null
        ? Response.status(Status.NOT_FOUND).type(MediaType.TEXT_PLAIN).entity(errorMsg).build()
        : Response.ok(content, mediaType).build();
    return resp;
  }

}
