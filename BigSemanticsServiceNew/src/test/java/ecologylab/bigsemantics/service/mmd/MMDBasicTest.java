package ecologylab.bigsemantics.service.mmd;

import ecologylab.bigsemantics.service.BasicTest;
import ecologylab.bigsemantics.service.ParamType;
import ecologylab.bigsemantics.service.RequestType;
import ecologylab.serialization.formatenums.StringFormat;

public class MMDBasicTest extends BasicTest
{

	public MMDBasicTest(String urlOrName, StringFormat format, ParamType paramType)
	{
		super(urlOrName, RequestType.MMD, format, paramType);
	}

}
