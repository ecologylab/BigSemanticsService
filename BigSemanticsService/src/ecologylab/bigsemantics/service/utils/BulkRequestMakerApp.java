package ecologylab.bigsemantics.service.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author quyin
 * 
 */
public class BulkRequestMakerApp
{

  /**
   * @param args
   * @throws IOException
   * @throws InterruptedException
   * @throws SecurityException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  public static void main(String[] args) throws IOException, InterruptedException,
      NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException
  {
    String serviceBase = "http://localhost:8080/BigSemanticsService/metadata.xml";
    Class<? extends AbstractBulkRequestMaker> brmClass = null;

    int i = 0;
    while (i < args.length)
    {
      if (args[i].equals("-c"))
      {
        String brmClassName = args[++i];
        try
        {
          brmClass = (Class<? extends AbstractBulkRequestMaker>) Class.forName(brmClassName);
        }
        catch (ClassNotFoundException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
          System.err.println("cannot find class: " + brmClassName);
          System.exit(-2);
        }
      }
      else if (args[i].equals("-b"))
      {
        serviceBase = args[++i];
      }
      else
      {
        break;
      }
      ++i;
    }

    if (args.length - i < 2)
    {
      System.err.println("args: <url-list-file> <num-of-threads>");
      System.err.println("options: -b <service-base>");
      System.err.println("         -c <bulk-request-maker-class-name>");
      System.exit(-1);
    }

    String urlListFile = args[i++];
    int nThreads = Integer.parseInt(args[i++]);

    List<String> urls = new ArrayList<String>();
    BufferedReader br = null;
    try
    {
      br = new BufferedReader(new FileReader(urlListFile));
      while (true)
      {
        String line = br.readLine();
        if (line == null)
          break;
        line = line.trim();
        if (line.length() > 0 && line.charAt(0) != '#')
        {
          urls.add(line);
        }
      }
    }
    finally
    {
      if (br != null)
        br.close();
    }

    Constructor<? extends AbstractBulkRequestMaker> cstr = brmClass.getConstructor(String.class);
    AbstractBulkRequestMaker brm = cstr.newInstance(serviceBase);
    brm.request(urls, true, nThreads);
  }

}
