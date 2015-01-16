package ecologylab.bigsemantics.service.resources;

import javax.ws.rs.core.Response;

import ecologylab.bigsemantics.service.ParamType;
import ecologylab.serialization.formatenums.StringFormat;

public class MMDStatusOK extends MMDBasicTest
{
	public MMDStatusOK(String urlOrName, StringFormat format, ParamType paramType)
	{
		super(urlOrName, format, paramType);
	}
	
	@Override
	public boolean doTest()
	{
		Response response = getServiceResponse();
		if (response.getStatus() == Response.Status.OK.ordinal())
		{
			String serviceMmd = response.readEntity(String.class);
			String localMmd = getMmdLocally();
			
			debug("Service MMD: " + serviceMmd);
			debug("Local MMD: " + localMmd);
			
			if (serviceMmd != null && localMmd != null && serviceMmd.equals(localMmd))
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
		MMDStatusOK t = new MMDStatusOK("http://ecologylab.net", StringFormat.XML, ParamType.URL);
		t.doTest();
	}

}
