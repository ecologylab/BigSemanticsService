package ecologylab.bigsemantics.service.crawler;

import java.io.IOException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * 
 * @author quyin
 */
public class LocalScholarlyArticleCrawlerApp extends BasicDocumentCrawlerApp
{

  private ScholarlyArticleExpander expander;

  private LocalDocumentCrawler     crawler;

  public LocalScholarlyArticleCrawlerApp(Configuration configs) throws IOException
  {
    super(configs);

    expander = new ScholarlyArticleExpander();

    int timeout = configs.getInt("timeout", 120) * 1000;
    crawler = new LocalDocumentCrawler(expander, timeout);
  }

  @Override
  protected AbstractDocumentCrawler getDocumentCrawler()
  {
    return crawler;
  }

  public static void main(String[] args) throws ConfigurationException, IOException
  {
    Configuration configs = new PropertiesConfiguration("properties");
    LocalScholarlyArticleCrawlerApp app = new LocalScholarlyArticleCrawlerApp(configs);
    app.crawl();
  }

}
