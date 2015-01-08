package ecologylab.bigsemantics.service.mmd;

import javax.ws.rs.core.Response;

import ecologylab.bigsemantics.service.ParamType;
import ecologylab.serialization.formatenums.StringFormat;

public class MMDNotFound extends MMDBasicTest
{

	public MMDNotFound(String urlOrName, StringFormat format, ParamType paramType)
	{
		super(urlOrName, format, paramType);
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
