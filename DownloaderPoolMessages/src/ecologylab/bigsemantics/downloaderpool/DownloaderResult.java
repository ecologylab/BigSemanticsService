package ecologylab.bigsemantics.downloaderpool;

import ecologylab.logging.LogPost;
import ecologylab.serialization.annotations.simpl_composite;
import ecologylab.serialization.annotations.simpl_inherit;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * The downloading result sent from a downloader to the controller.
 * 
 * @author quyin
 */
@simpl_inherit
public class DownloaderResult extends BasicResponse
{

  /**
   * The state of the result.
   * 
   * @author quyin
   */
  public static enum State
  {

    /**
     * The request is fulfilled.
     */
    OK,

    /**
     * The request encountered I/O errors, such as network connectivity issues.
     */
    ERR_IO,

    /**
     * The request encountered HTTP protocol errors, including but not limited to 4xx, 5xx status
     * codes.
     */
    ERR_PROTOCOL,

    /**
     * The request encountered content errors, such as unsupported MIME types or unrecognized
     * encoding.
     */
    ERR_CONTENT,

    /**
     * We have been banned by this domain.
     */
    ERR_BANNED,

    /**
     * The request encountered other errors.
     */
    ERR_OTHER,

  }

  /**
   * Used to associate the result with a task.
   */
  @simpl_scalar
  private String  taskId;

  /**
   * The state of the result.
   */
  @simpl_scalar
  private State   state;

  /**
   * Extra description of the content, e.g. how page content is compressed (e.g. zipped or not)
   * and/or encoded (e.g. bse64 or not).
   */
  @simpl_scalar
  private String  contentDescription;

  @simpl_composite
  private LogPost logPost;

  public String getTaskId()
  {
    return taskId;
  }

  public void setTaskId(String taskId)
  {
    this.taskId = taskId;
  }

  public State getState()
  {
    return state;
  }

  public void setState(State state)
  {
    this.state = state;
  }

  public String getContentDescription()
  {
    return contentDescription;
  }

  public void setContentDescription(String contentDescription)
  {
    this.contentDescription = contentDescription;
  }

  public LogPost getLogPost()
  {
    return logPost;
  }

  public void setLogPost(LogPost logPost)
  {
    this.logPost = logPost;
  }

}
