package ecologylab.bigsemantics.service.metadata;

import javax.ws.rs.core.Response;

import ecologylab.serialization.formatenums.StringFormat;

public class MetadataBadRequest extends MetadataBasicTest
{

	public MetadataBadRequest(String uri, StringFormat format)
	{
		super(uri, format);
	}
	
	@Override
	public boolean doTest()
	{
		Response response = getServiceResponse();
		if (response.getStatus() == Response.Status.BAD_REQUEST.ordinal())
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
