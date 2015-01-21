package ecologylab.bigsemantics.logging;

import java.util.ArrayList;
import java.util.List;

import ecologylab.serialization.annotations.simpl_collection;

/**
 * 
 * @author quyin
 */
public class ServiceLogRecordCollection
{

  @simpl_collection("log")
  List<ServiceLogRecord> logs;
  
  public void addLogRecord(ServiceLogRecord logRecord)
  {
    if (logs == null)
    {
      logs = new ArrayList<ServiceLogRecord>();
    }
    logs.add(logRecord);
  }

}
