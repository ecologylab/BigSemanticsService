package ecologylab.bigsemantics.service.metadata;

import ecologylab.bigsemantics.service.BasicTest;
import ecologylab.bigsemantics.service.RequestType;
import ecologylab.serialization.formatenums.StringFormat;

public class MetadataBasicTest extends BasicTest
{

	protected MetadataBasicTest(String uri, StringFormat format)
	{
		super(uri, RequestType.METADATA, format);
	}

}
