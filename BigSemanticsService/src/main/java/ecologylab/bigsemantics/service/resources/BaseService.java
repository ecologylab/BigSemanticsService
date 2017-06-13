package ecologylab.bigsemantics.service.resources;

import javax.ws.rs.QueryParam;

/**
 * 
 * @author quyin
 */
public class BaseService
{

  @QueryParam("aid")
  String appId;

  @QueryParam("aver")
  String appVer;

  @QueryParam("uid")
  String userId;

  @QueryParam("sid")
  String sessionId;

  String keyValuePair(String key, String value)
  {
    return keyValuePair(key, value, true);
  }

  String keyValuePair(String key, String value, boolean valueIsString)
  {
    if (key == null || value == null) return null;
    return valueIsString ? String.format("\"%s\":\"%s\"", key, value) : String.format("\"%s\":%s", key, value);
  }
  
  String join(String[] parts, String sep)
  {
    StringBuilder result = new StringBuilder();
    for (String part : parts)
    {
      if (part != null)
      {
        result.append(result.length() == 0 ? "" : ",").append(part);
      }
    }
    return result.toString();
  }
  
  String reqStr()
  {
    String reqBody = join(new String[] {
        keyValuePair("appId", appId),
        keyValuePair("appVer", appVer),
        keyValuePair("userId", userId),
        keyValuePair("sessionId", sessionId),
      }, ",");
    return reqBody.length() > 0 ? "{" + reqBody + "}" : null;
  }
  
}
