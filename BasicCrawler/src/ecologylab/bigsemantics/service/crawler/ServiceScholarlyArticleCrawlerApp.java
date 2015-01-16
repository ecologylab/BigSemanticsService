package ecologylab.bigsemantics.service.crawler;

import java.io.IOException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * 
 * @author quyin
 */
public class ServiceScholarlyArticleCrawlerApp extends BasicDocumentCrawlerApp
{

  private ScholarlyArticleExpander expander;

  private ServiceDocumentCrawler   crawler;

  public ServiceScholarlyArticleCrawlerApp(Configuration configs) throws IOException
  {
    super(configs);

    expander = new ScholarlyArticleExpander();

    String metadataServiceUri = configs.getString("metadata_service_uri");
    int timeout = configs.getInt("timeout", 120) * 1000;
    boolean reload = configs.getBoolean("reload", false);

    crawler = new ServiceDocumentCrawler(expander,
                                         metadataServiceUri,
                                         timeout,
                                         reload);
  }

  @Override
  protected AbstractDocumentCrawler getDocumentCrawler()
  {
    return crawler;
  }

  public static void main(String[] args) throws ConfigurationException, IOException
  {
    Configuration configs = new PropertiesConfiguration("properties");
    ServiceScholarlyArticleCrawlerApp app = new ServiceScholarlyArticleCrawlerApp(configs);
    app.crawl();
  }

}
