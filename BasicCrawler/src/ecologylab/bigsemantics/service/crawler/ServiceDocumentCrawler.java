package ecologylab.bigsemantics.service.crawler;

import java.io.IOException;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import ecologylab.bigsemantics.collecting.DownloadStatus;
import ecologylab.bigsemantics.collecting.SemanticsSessionScope;
import ecologylab.bigsemantics.metadata.MetadataDeserializationHookStrategy;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * 
 * @author quyin
 */
public class ServiceDocumentCrawler extends AbstractDocumentCrawler
{

  private static Logger logger = LoggerFactory.getLogger(ServiceDocumentCrawler.class);

  private String        metadataServiceUri;

  private int           timeout;

  private boolean       reload;

  private Client        client;

  /**
   * 
   * @param metadataServiceUri
   * @param timeout
   * @param reload
   */
  public ServiceDocumentCrawler(DocumentExpander expander,
                                String metadataServiceUri,
                                int timeout,
                                boolean reload)
  {
    super(expander);

    this.metadataServiceUri = metadataServiceUri;
    this.timeout = timeout;
    this.reload = reload;

    client = Client.create();
    client.setFollowRedirects(true);
    client.setConnectTimeout(this.timeout);
  }

  @Override
  protected Document getDocument(String uri) throws IOException
  {
    String encodedUri = URLEncoder.encode(uri, Charsets.UTF_8.name());
    String reqUri = String.format("%s?url=%s%s",
                                  metadataServiceUri,
                                  encodedUri,
                                  reload ? "&reload=true" : "");

    String serial = null;
    logger.info("Accessing [{}] using rddequest [{}]...", uri, reqUri);
    WebResource resource = client.resource(reqUri);
    ClientResponse resp = resource == null ? null : resource.get(ClientResponse.class);
    serial = resp == null ? null : resp.getEntity(String.class);

    if (serial == null || serial.length() == 0)
    {
      throw new IOException("Empty response for [" + uri + "] using [" + reqUri + "].");
    }
    if (checkForBanning(uri, serial))
    {
      throw new IOException("Banned: [" + uri + "]!");
    }
    logger.info("Metadata received for [{}], length={}.", uri, serial.length());

    SimplTypesScope metadataTScope = getMetadataTScope();
    SemanticsSessionScope sessionScope = getSemanticsSessionScope();
    MetadataDeserializationHookStrategy hookStrategy =
        new MetadataDeserializationHookStrategy(sessionScope);

    Object obj = null;
    try
    {
      obj = metadataTScope.deserialize(serial, hookStrategy, StringFormat.XML);
    }
    catch (SIMPLTranslationException e)
    {
      logger.error("Invalid XML received for [" + uri + "] using [" + reqUri + "].", e);
      logger.info("XML:\n" + serial);
      throw new IOException("Error obtaining metadata from the service", e);
    }

    if (obj == null)
    {
      throw new IOException("Weird: deserialized object is null for [" + uri + "]!");
    }

    if (!(obj instanceof Document))
    {
      throw new IOException("Received non-document object for [" + uri + "].");
    }

    Document doc = (Document) obj;
    if (doc.getDownloadStatus() == DownloadStatus.RECYCLED)
    {
      throw new IOException("Document[" + uri + "] has been recycled.");
    }
    return doc;
  }

  protected boolean checkForBanning(String uri, String serial)
  {
    return false;
  }

}
