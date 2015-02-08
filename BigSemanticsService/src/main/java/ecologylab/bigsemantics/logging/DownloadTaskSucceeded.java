package ecologylab.bigsemantics.logging;

import ecologylab.logging.LogEvent;
import ecologylab.serialization.annotations.simpl_inherit;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * When a dpool task is done successfully.
 * 
 * @author quyin
 */
@simpl_inherit
public class DownloadTaskSucceeded extends LogEvent
{
  
  @simpl_scalar
  private String downloaderId;
  
  @simpl_scalar
  private int contentLength;

  public DownloadTaskSucceeded()
  {
    super();
  }

  public String getDownloaderId()
  {
    return downloaderId;
  }

  public void setDownloaderId(String downloaderId)
  {
    this.downloaderId = downloaderId;
  }

  public int getContentLength()
  {
    return contentLength;
  }

  public void setContentLength(int contentLength)
  {
    this.contentLength = contentLength;
  }

}
