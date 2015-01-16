package ecologylab.bigsemantics.service.crawler;

import ecologylab.bigsemantics.metadata.builtins.Document;

/**
 * 
 * @author quyin
 */
public interface DocumentExpander
{
  
  /**
   * Expand a document and queue results to the crawler.
   * 
   * @param crawler
   * @param doc
   */
  void expand(AbstractDocumentCrawler crawler, Document doc);

}
