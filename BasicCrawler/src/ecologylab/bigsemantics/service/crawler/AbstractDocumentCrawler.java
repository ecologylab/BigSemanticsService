package ecologylab.bigsemantics.service.crawler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import ecologylab.bigsemantics.collecting.SemanticsSessionScope;
import ecologylab.bigsemantics.cyberneko.CybernekoWrapper;
import ecologylab.bigsemantics.generated.library.RepositoryMetadataTypesScope;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.SimplTypesScope;

/**
 * Crawls given documents and their linked documents as indicated.
 * 
 * @author quyin
 */
public abstract class AbstractDocumentCrawler implements ResourceCrawler<Document>
{

  private SimplTypesScope                 metadataTScope;

  private SemanticsSessionScope           semanticsSessionScope;

  private DocumentExpander                expander;

  private ConcurrentLinkedQueue<Document> ongoingDocs;

  private Set<String>                     seenDocs;

  private int                             nQueued;

  private int                             nAccessed;

  private int                             nSucc;

  /**
   * The constructor.
   */
  public AbstractDocumentCrawler(DocumentExpander expander)
  {
    super();

    this.expander = expander;

    metadataTScope = RepositoryMetadataTypesScope.get();
    semanticsSessionScope = new SemanticsSessionScope(metadataTScope, CybernekoWrapper.class);
    ongoingDocs = new ConcurrentLinkedQueue<Document>();
    seenDocs = new HashSet<String>();
  }

  protected SimplTypesScope getMetadataTScope()
  {
    return metadataTScope;
  }

  protected SemanticsSessionScope getSemanticsSessionScope()
  {
    return semanticsSessionScope;
  }

  @Override
  public void queue(String uri)
  {
    if (uri == null)
      throw new NullPointerException("URI should not be null.");

    nQueued++;

    ParsedURL purl = ParsedURL.getAbsolute(uri);
    Document doc = semanticsSessionScope.getOrConstructDocument(purl);
    uri = doc.getLocation().toString();
    if (!seenDocs.contains(uri))
    {
      ongoingDocs.offer(doc);
      seenDocs.add(uri);
    }
  }

  @Override
  public boolean hasNext()
  {
    return ongoingDocs.size() > 0;
  }

  @Override
  public Document next() throws IOException
  {
    if (hasNext())
    {
      Document doc = ongoingDocs.poll();
      ParsedURL purl = doc.getLocation();
      if (purl != null)
      {
        nAccessed++;
        doc = getDocument(purl.toString());
        nSucc++;
        expand(doc);
        return doc;
      }
    }
    return null;
  }

  /**
   * Get the Document object given its URI.
   * 
   * @param uri
   *          The URI of the document.
   * @return The retrieved Document object.
   * @throws IOException
   */
  abstract protected Document getDocument(String uri) throws IOException;

  @Override
  public void expand(Document doc)
  {
    expander.expand(this, doc);
  }

  @Override
  public int countQueued()
  {
    return nQueued;
  }

  @Override
  public int countWaiting()
  {
    return ongoingDocs.size();
  }

  @Override
  public int countAccessed()
  {
    return nAccessed;
  }

  @Override
  public int countSuccess()
  {
    return nSucc;
  }

  @Override
  public int countFailure()
  {
    return nAccessed - nSucc;
  }

}
