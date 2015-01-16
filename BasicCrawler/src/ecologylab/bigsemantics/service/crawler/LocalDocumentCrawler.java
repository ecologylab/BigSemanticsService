package ecologylab.bigsemantics.service.crawler;

import java.io.IOException;

import ecologylab.bigsemantics.collecting.DownloadStatus;
import ecologylab.bigsemantics.collecting.SemanticsSessionScope;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metadata.builtins.DocumentClosure;
import ecologylab.generic.Continuation;
import ecologylab.net.ParsedURL;

/**
 * 
 * @author quyin
 */
public class LocalDocumentCrawler extends AbstractDocumentCrawler
{

  private int timeout;

  public LocalDocumentCrawler(DocumentExpander expander, int timeout)
  {
    super(expander);
    this.timeout = timeout;
  }

  @Override
  protected Document getDocument(String uri) throws IOException
  {
    SemanticsSessionScope sss = getSemanticsSessionScope();

    ParsedURL purl = ParsedURL.getAbsolute(uri);
    Document doc = sss.getOrConstructDocument(purl);
    DocumentClosure closure = doc.getOrConstructClosure();

    final Object lock = new Object();
    synchronized (lock)
    {
      closure.addContinuation(new Continuation<DocumentClosure>()
      {
        @Override
        public void callback(DocumentClosure closure)
        {
          synchronized (lock)
          {
            lock.notifyAll();
          }
        }
      });
      closure.queueDownload();

      try
      {
        lock.wait(timeout);
      }
      catch (InterruptedException e)
      {
        // no op
      }
    }

    doc = closure.getDocument();
    if (doc == null || doc.getDownloadStatus() != DownloadStatus.DOWNLOAD_DONE)
      throw new IOException("Error downloading [" + uri + "].");

    return doc;
  }

}
