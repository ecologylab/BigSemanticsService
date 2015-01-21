package ecologylab.bigsemantics.logging;

import ecologylab.logging.LogEvent;
import ecologylab.serialization.annotations.simpl_inherit;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * When a dpool task is assigned to a downloader.
 * 
 * @author quyin
 */
@simpl_inherit
public class DpoolTaskAssigned extends LogEvent
{
  
  @simpl_scalar
  private String downloaderId;

  public DpoolTaskAssigned()
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

}
