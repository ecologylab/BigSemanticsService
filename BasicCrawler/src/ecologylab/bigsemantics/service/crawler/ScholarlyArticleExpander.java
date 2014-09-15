package ecologylab.bigsemantics.service.crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.generated.library.person.author.*;
import ecologylab.bigsemantics.generated.library.creativeWork.scholarlyArticle.ScholarlyArticle;
import ecologylab.bigsemantics.metadata.MetadataClassDescriptor;
import ecologylab.bigsemantics.metadata.MetadataFieldDescriptor;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.net.ParsedURL;

/**
 * Expand scholarly articles using authors, references, and citations.
 * 
 * @author quyin
 */
public class ScholarlyArticleExpander extends AbstractDocumentExpander
{

  private static Logger logger = LoggerFactory.getLogger(ScholarlyArticleExpander.class);

  @Override
  public void expand(AbstractDocumentCrawler crawler, Document doc)
  {
    if (doc instanceof ScholarlyArticle)
    {
      ScholarlyArticle article = (ScholarlyArticle) doc;

//      if (expandConnectedCollection(crawler, article, "authors", Author.class))
//      {
//        for (Author author : article.getAuthors())
//        {
//          queue(crawler, author.getCreativeWorks());
//        }
//      }

      queue(crawler, article.getReferences());
      queue(crawler, article.getCitations());
    }
  }

  protected <T extends Document> void queue(AbstractDocumentCrawler crawler, List<T> docs)
  {
    if (docs != null)
    {
      for (Document doc : docs)
      {
        if (doc != null && doc.getLocation() != null && doc instanceof ScholarlyArticle)
        {
          crawler.queue(doc.getLocation().toString());
        }
      }
    }
  }

  protected boolean expandConnectedCompsite(AbstractDocumentCrawler crawler,
                                            Document doc,
                                            String compositeName,
                                            Class<? extends Document> compositeClass)
  {
    MetadataClassDescriptor cd = doc.getMetadataClassDescriptor();
    MetadataFieldDescriptor fd = cd.getFieldDescriptorByFieldName(compositeName);
    Object v = fd.getValue(doc);
    if (v != null && v instanceof Document)
    {
      Document d = getDocument(crawler, (Document) v);
      if (d != null && compositeClass.isAssignableFrom(d.getClass()))
      {
        fd.set(doc, d);
        return true;
      }
    }
    return false;
  }

  protected boolean expandConnectedCollection(AbstractDocumentCrawler crawler,
                                              Document doc,
                                              String collectionName,
                                              Class<? extends Document> elementClass)
  {
    MetadataClassDescriptor cd = doc.getMetadataClassDescriptor();
    MetadataFieldDescriptor fd = cd.getFieldDescriptorByFieldName(collectionName);
    Object v = fd.getValue(doc);
    if (v != null && v instanceof List)
    {
      List list = (List) v;
      List newList = new ArrayList();
      for (int i = 0; i < list.size(); ++i)
      {
        Object e = list.get(i);
        if (e != null && e instanceof Document)
        {
          Document d = getDocument(crawler, (Document) e);
          if (d != null && elementClass.isAssignableFrom(d.getClass()))
          {
            newList.add(d);
          }
        }
      }
      if (newList.size() > 0)
      {
        fd.set(doc, newList);
        return true;
      }
    }
    return false;
  }

  /**
   * Helper method that tries to get an expanded (downloaded and extracted) form of the input doc.
   * 
   * @param crawler
   * @param doc
   * @return
   */
  <T extends Document> T getDocument(AbstractDocumentCrawler crawler, T doc)
  {
    if (doc != null)
    {
      ParsedURL loc = doc.getLocation();
      String url = loc == null ? null : loc.toString();
      if (url != null)
      {
        Document expanded = null;
        try
        {
          expanded = getDocument(crawler, url);
        }
        catch (IOException e)
        {
          // no op
        }
        if (expanded != null && doc.getClass().isAssignableFrom(expanded.getClass()))
          return (T) expanded;
      }
    }
    return doc;
  }

  protected Document getDocument(AbstractDocumentCrawler crawler, String url) throws IOException
  {
    Document doc = crawler.getDocument(url);
    return doc;
  }

}
