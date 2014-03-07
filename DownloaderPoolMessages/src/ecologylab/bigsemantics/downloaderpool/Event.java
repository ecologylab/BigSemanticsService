package ecologylab.bigsemantics.downloaderpool;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ecologylab.serialization.annotations.simpl_collection;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * Recording events.
 * 
 * @author quyin
 */
public class Event
{

  static SimpleDateFormat dateFormat;

  static
  {
    dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
  }

  @simpl_scalar
  private String          category;

  @simpl_scalar
  private String          name;

  @simpl_scalar
  private String          timestamp;

  @simpl_collection("param")
  private List<String>    params;

  private Object          lockParams = new Object();

  public Event()
  {
    this("<uninitialized event>");
  }

  public Event(String name)
  {
    this(null, name);
  }

  public Event(String category, String name)
  {
    this.category = category;
    this.name = name;
    this.timestamp = dateFormat.format(new Date());
  }

  public String getCategory()
  {
    return category;
  }

  public void setCategory(String category)
  {
    this.category = category;
  }

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getTimestamp()
  {
    return timestamp;
  }

  public void setTimestamp(String timestamp)
  {
    this.timestamp = timestamp;
  }

  public List<String> getParams()
  {
    return params;
  }

  public void addParam(String param)
  {
    if (params == null)
    {
      synchronized (lockParams)
      {
        if (params == null)
        {
          params = new ArrayList<String>();
        }
      }
    }
    if (params != null)
    {
      params.add(param);
    }
  }

  public void setParams(List<String> params)
  {
    this.params = params;
  }

}
