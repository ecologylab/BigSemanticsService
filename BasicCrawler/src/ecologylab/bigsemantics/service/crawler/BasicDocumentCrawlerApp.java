package ecologylab.bigsemantics.service.crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.metadata.builtins.Document;

/**
 * 
 * @author quyin
 */
public abstract class BasicDocumentCrawlerApp
{

  private static Logger  logger = LoggerFactory.getLogger(BasicDocumentCrawlerApp.class);

  private String         inputFileName;

  private String         outputFileName;

  private int            maxCount;

  private int            maxTime;

  private BufferedWriter writer;

  public BasicDocumentCrawlerApp(Configuration configs) throws IOException
  {
    inputFileName = configs.getString("input_url_list_file");
    outputFileName = configs.getString("output_file");
    maxCount = configs.getInt("max_count", Integer.MAX_VALUE);
    maxTime = configs.getInt("max_time", Integer.MAX_VALUE) * 1000;

    writer = new BufferedWriter(new FileWriter(outputFileName));
  }

  public List<String> readLines(String filename) throws IOException
  {
    List<String> result = new ArrayList<String>();

    BufferedReader br = new BufferedReader(new FileReader(filename));
    while (true)
    {
      String line = br.readLine();
      if (line == null)
        break;
      line = line.trim();
      if (line.length() > 0)
      {
        result.add(line);
      }
    }
    br.close();

    return result;
  }

  abstract protected AbstractDocumentCrawler getDocumentCrawler();

  protected void output(Document d)
  {
    if (d != null && d.getLocation() != null)
    {
      try
      {
        writer.write(d.getLocation().toString());
        writer.newLine();
        writer.flush();
      }
      catch (IOException e)
      {
        logger.error("Error when saving result.", e);
      }
    }
  }

  public void crawl() throws IOException
  {
    List<String> seeds = readLines(inputFileName);

    ResourceCrawler<Document> crawler = getDocumentCrawler();
    for (String seedUri : seeds)
      crawler.queue(seedUri);

    long t0 = System.currentTimeMillis();
    while (crawler.hasNext())
    {
      try
      {
        Document d = crawler.next();
        output(d);
      }
      catch (IOException e)
      {
        logger.error("Error when crawling the next document.", e);
      }

      if ((System.currentTimeMillis() - t0) > maxTime)
        break;

      if (crawler.countSuccess() >= maxCount)
        break;
    }
    
    logger.info("total time: {} millisec", System.currentTimeMillis() - t0);
    logger.info("total success: " + crawler.countSuccess());
  }

}
