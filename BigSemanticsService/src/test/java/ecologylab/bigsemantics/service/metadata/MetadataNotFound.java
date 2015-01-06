package ecologylab.bigsemantics.service.metadata;

import javax.ws.rs.core.Response;

import ecologylab.serialization.formatenums.StringFormat;

public class MetadataNotFound extends MetadataBasicTest
{

	public MetadataNotFound(String uri, StringFormat format)
	{
		super(uri, format);
	}

	@Override
	public boolean doTest()
	{
		Response response = getServiceResponse();
		if (response.getStatus() == Response.Status.NOT_FOUND.ordinal())
		{
			debug("Test case passed");
			return true;
		}
		else
		{
			warning("Test case failed");
			return false;
		}
	}
}
