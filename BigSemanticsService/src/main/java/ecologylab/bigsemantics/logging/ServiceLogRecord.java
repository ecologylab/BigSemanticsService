package ecologylab.bigsemantics.logging;

import java.util.Date;

import ecologylab.bigsemantics.Utils;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.annotations.simpl_inherit;
import ecologylab.serialization.annotations.simpl_scalar;

@simpl_inherit
public class ServiceLogRecord extends DocumentLogRecord
{

  @simpl_scalar
  private String requesterIp;

  @simpl_scalar
  private Date   requestTime;

  @simpl_scalar
  private String requestUrl;

  @simpl_scalar
  private String clientAttachedId;

  @simpl_scalar
  private int    responseCode;

  @Override
  public void setDocumentLocation(ParsedURL location)
  {
    super.setDocumentLocation(location);
    setId(Utils.getLocationHash(location));
  }

  public String getRequesterIp()
  {
    return requesterIp;
  }

  public void setRequesterIp(String requesterIp)
  {
    this.requesterIp = requesterIp;
  }

  public Date getRequestTime()
  {
    return requestTime;
  }

  public void setRequestTime(Date requestTime)
  {
    this.requestTime = requestTime;
  }

  public String getRequestUrl()
  {
    return requestUrl;
  }

  public void setRequestUrl(String requestUrl)
  {
    this.requestUrl = requestUrl;
  }

  public String getClientAttachedId()
  {
    return clientAttachedId;
  }

  public void setClientAttachedId(String clientAttachedId)
  {
    this.clientAttachedId = clientAttachedId;
  }

  public int getResponseCode()
  {
    return responseCode;
  }

  public void setResponseCode(int responseCode)
  {
    this.responseCode = responseCode;
  }

  @Override
  public String toString()
  {
    return String.format("%s[%s]",
                         ServiceLogRecord.class.getSimpleName(),
                         requestUrl == null ? "NullLocation" : requestUrl.toString());
  }

}
