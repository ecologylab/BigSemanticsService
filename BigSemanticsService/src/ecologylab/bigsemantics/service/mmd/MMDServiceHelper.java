/**
 * 
 */
package ecologylab.bigsemantics.service.mmd;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.collecting.DownloadStatus;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metametadata.MetaMetadata;
import ecologylab.bigsemantics.metametadata.MetaMetadataCompositeField;
import ecologylab.bigsemantics.service.SemanticServiceErrorMessages;
import ecologylab.bigsemantics.service.SemanticsServiceScope;
import ecologylab.bigsemantics.service.ServiceUtils;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * Helper class for mmd.xml and mmd.json root resources
 * 
 * @author ajit
 */
public class MMDServiceHelper implements MMDServiceParamNames
{

  public static final int                       MAX_CACHED_URL        = 1000;

  static Logger                                 logger;

  static SemanticsServiceScope                  semanticsServiceScope = SemanticsServiceScope.get();

  static LinkedHashMap<ParsedURL, MetaMetadata> mmdByUrl;

  static
  {
    logger = LoggerFactory.getLogger(MMDServiceHelper.class);
    mmdByUrl = new LinkedHashMap<ParsedURL, MetaMetadata>(MAX_CACHED_URL + 1)
    {
      public boolean removeEldestEntry(Map.Entry eldestEntry)
      {
        return this.size() > MAX_CACHED_URL;
      }
    };
  }
  
  public static Response getMmdResponse(String url,
                                        String name,
                                        String callback,
                                        String withUrl,
                                        UriInfo uriInfo,
                                        StringFormat format)
  {
    Response resp = null;
    if (url != null)
    {
      ParsedURL purl = ParsedURL.getAbsolute(url);
      if (purl != null)
      {
        MetaMetadata mmd = getMmdByUrl(purl);
        if (mmd != null)
        {
          UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder().queryParam(NAME, mmd.getName());
          if (callback != null)
          {
            uriBuilder = uriBuilder.queryParam(CALLBACK, callback);
            if (withUrl != null)
            {
              // Here, note that whatever the value of withurl is, in the redirection we use the
              // real URL. in this way you don't need to repeat the URL, since it is already
              // available through the url parameter.
              uriBuilder = uriBuilder.queryParam(WITH_URL, ServiceUtils.urlencode(url));
            }
          }
          URI nameURI = uriBuilder.build();
          resp = Response.status(Status.SEE_OTHER).location(nameURI).build();
        }
      }
    }
    else if (name != null)
    {
      MetaMetadata mmd = getMmdByName(name);
      if (mmd != null)
      {
        String mmdJson = ServiceUtils.serialize(mmd, format);
        String respString = mmdJson;
        if (callback != null)
        {
          String locParam = "";
          if (withUrl != null)
          {
            logger.info("withUrl = {}", withUrl);
            locParam = "\"" + withUrl + "\", ";
          }
          respString = callback + "(" + locParam + mmdJson + ");";
        }
        resp = Response.status(Status.OK).entity(respString).build();
      }
    }
    else
    {
      resp = Response
          .status(Status.BAD_REQUEST)
          .entity(SemanticServiceErrorMessages.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN)
          .build();
    }

    if (resp == null)
    {
      resp = Response
          .status(Status.NOT_FOUND)
          .entity(SemanticServiceErrorMessages.METAMETADATA_NOT_FOUND)
          .type(MediaType.TEXT_PLAIN)
          .build();
    }
    
    return resp;
  }

  public static MetaMetadata getMmdByName(String mmdName)
  {
    MetaMetadata docMM = semanticsServiceScope.getMetaMetadataRepository().getMMByName(mmdName);
    return docMM;
  }

  public static MetaMetadata getMmdByUrl(ParsedURL url)
  {
    MetaMetadata docMM = null;

    if (mmdByUrl.containsKey(url))
    {
      // check in cache
      docMM = mmdByUrl.get(url);
    }
    else
    {
      // check if we already have a Document associated with this URL. if so, we want to use the
      // meta-metadata for that Document, because it may get changed when we make the HTTP
      // connection.
      Document doc = semanticsServiceScope.getOrConstructDocument(url);
      if (doc != null && doc.getDownloadStatus() == DownloadStatus.DOWNLOAD_DONE)
      {
        MetaMetadataCompositeField docMMComposite = doc.getMetaMetadata();
        if (docMMComposite instanceof MetaMetadata)
        {
          docMM = (MetaMetadata) docMMComposite;
        }
      }

      if (docMM == null)
      {
        // if no downloaded Document found, we can only use the URL to find a meta-metadata.
        docMM = semanticsServiceScope.getMetaMetadataRepository().getDocumentMM(url);
        if (docMM != null)
        {
          // cache the mmd
          synchronized (mmdByUrl)
          {
            // it's fine to put the same mmd twice, but we want to sync all put operations to
            // prevent concurrent modification.
            mmdByUrl.put(url, docMM);
          }
        }
      }
    }
    
    return docMM;
  }

}
