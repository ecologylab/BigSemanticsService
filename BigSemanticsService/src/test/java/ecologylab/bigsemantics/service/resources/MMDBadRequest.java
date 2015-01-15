package ecologylab.bigsemantics.service.resources;

import javax.ws.rs.core.Response;

import ecologylab.bigsemantics.service.ParamType;
import ecologylab.serialization.formatenums.StringFormat;

public class MMDBadRequest extends MMDBasicTest
{

	public MMDBadRequest(String urlOrName, StringFormat format, ParamType paramType)
	{
		super(urlOrName, format, paramType);
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
