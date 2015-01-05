package ecologylab.bigsemantics.service.crawler;

import java.io.IOException;

/**
 * A general framework for crawling resources.
 * 
 * @author quyin
 */
public interface ResourceCrawler<T>
{

  /**
   * Queue a resource with the given URI.
   * 
   * @param uri
   */
  void queue(String uri);

  /**
   * If the crawler has more resources to crawl.
   * 
   * @return true if there are still resources to crawl.
   */
  boolean hasNext();

  /**
   * Retrieve the next resource.
   * 
   * @return The next crawled resource.
   * @throws IOException
   *           If the resource cannot be accessed.
   */
  T next() throws IOException;

  /**
   * Expand a given resource.
   * 
   * @param resource
   */
  void expand(T resource);
  
  /**
   * @return The number of resources queued.
   */
  int countQueued();

  /**
   * @return The number of resources that are to be crawled.
   */
  int countWaiting();

  /**
   * @return The number of resources that have been accessed.
   */
  int countAccessed();

  /**
   * @return The number of resources that have been accessed successfully.
   */
  int countSuccess();

  /**
   * @return The number of resources that have been accessed unsuccessfully.
   */
  int countFailure();

}