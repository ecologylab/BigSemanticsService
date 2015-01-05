/**
 * 
 */
package ecologylab.bigsemantics.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import ecologylab.bigsemantics.collecting.DownloadStatus;
import ecologylab.bigsemantics.collecting.SemanticsSessionScope;
import ecologylab.bigsemantics.cyberneko.CybernekoWrapper;
import ecologylab.bigsemantics.generated.library.RepositoryMetadataTypesScope;
import ecologylab.bigsemantics.metadata.Metadata;
import ecologylab.bigsemantics.metadata.MetadataComparator;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metadata.builtins.DocumentClosure;
import ecologylab.bigsemantics.metametadata.MetaMetadata;
import ecologylab.generic.Continuation;
import ecologylab.generic.Debug;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.SimplTypesScope.GRAPH_SWITCH;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * Base test case for receiving response from service and comparing it with one generated locally
 * 
 * @author ajit
 * 
 */

public class BasicTest extends Debug implements Continuation<DocumentClosure>
{
	private static final String						SERVICE_LOC	= "http://localhost:8080/ecologylabSemanticService/";

	private static SimplTypesScope				scope;

	private static SemanticsSessionScope	semanticsScope;

	private static Client									client;

	private String												param;

	private ParamType											paramType;
	
	private StringFormat									format;

	private String												requestUri;

	static
	{
		SimplTypesScope.graphSwitch = GRAPH_SWITCH.ON;
		scope = RepositoryMetadataTypesScope.get();
		semanticsScope = new SemanticsSessionScope(scope, CybernekoWrapper.class);

		client = ClientBuilder.newClient();
	}

	protected BasicTest(String uriOrName, RequestType requestType, StringFormat format)
	{
		this(uriOrName, requestType, format, ParamType.URL);
	}

	protected BasicTest(String uriOrName, RequestType requestType, StringFormat format,
			ParamType paramType)
	{
		this.param = uriOrName;
		this.paramType = paramType;
		this.format = format;
		try
		{
			this.requestUri = SERVICE_LOC + requestType.toString().toLowerCase() + "."
					+ format.toString().toLowerCase() + "?" + paramType.toString().toLowerCase() + "="
					+ URLEncoder.encode(uriOrName, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
	}

	protected Response getServiceResponse()
	{
	  WebTarget target = client.target(requestUri);
	  Builder builder = target.request();
	  Response resp = builder.get();
	  return resp;
	}

	protected Metadata deserializeMetadataResponse(String entity, StringFormat format)
	{
		try
		{
			return (Metadata) scope.deserialize(entity, format);
		}
		catch (SIMPLTranslationException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	protected Metadata getMetadataLocally()
	{
		Document document = semanticsScope.getOrConstructDocument(ParsedURL.getAbsolute(param));
		DocumentClosure documentClosure = document.getOrConstructClosure();

		if (documentClosure.getDownloadStatus() != DownloadStatus.DOWNLOAD_DONE)
		{
			synchronized (this)
			{
				documentClosure.addContinuation(this);
				documentClosure.queueDownload();
				semanticsScope.getDownloadMonitors().requestStops();
				try
				{
					wait();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
		semanticsScope.getDownloadMonitors().stop(false);
		return documentClosure.getDocument();
	}

	@Override
	public synchronized void callback(DocumentClosure o)
	{
		notify();
	}

	protected String getMmdLocally()
	{
		MetaMetadata localMmd = (paramType == ParamType.NAME) ? semanticsScope
				.getMetaMetadataRepository().getMMByName(param) : semanticsScope
				.getMetaMetadataRepository().getDocumentMM(ParsedURL.getAbsolute(param));
				
		if (localMmd != null)
		{
			try
			{
				return SimplTypesScope.serialize(localMmd, format).toString();
			}
			catch (SIMPLTranslationException e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}

	protected boolean doTest()
	{
		Response response = getServiceResponse();
		if (response.getStatus() == Response.Status.OK.ordinal())
		{
			Metadata serviceMetadata = deserializeMetadataResponse(response.readEntity(String.class),
					StringFormat.XML);

			Metadata localMetadata = getMetadataLocally();

			MetadataComparator comparator = new MetadataComparator();
			if (comparator.compare(serviceMetadata, localMetadata) == 0)
			{
				debug("Test case passed");
				return true;
			}
		}
		warning("Test case failed");
		return false;
	}

	public static void main(String[] args)
	{
		BasicTest t = new BasicTest("http://ecologylab.net", RequestType.METADATA, StringFormat.XML);
		t.doTest();
	}
}