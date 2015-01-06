package ecologylab.bigsemantics.service;

import java.util.ArrayList;

import ecologylab.bigsemantics.service.metadata.MetadataBadRequest;
import ecologylab.bigsemantics.service.metadata.MetadataNotFound;
import ecologylab.bigsemantics.service.metadata.MetadataOK;
import ecologylab.bigsemantics.service.mmd.MMDBadRequest;
import ecologylab.bigsemantics.service.mmd.MMDNotFound;
import ecologylab.bigsemantics.service.mmd.MMDStatusOK;
import ecologylab.serialization.formatenums.StringFormat;

public class TestRunner
{
	ArrayList<BasicTest>	testSuite	= new ArrayList<BasicTest>();

	public TestRunner()
	{
		testSuite.add(new MetadataOK("http://ecologylab.net", StringFormat.XML));
		
		testSuite.add(new MetadataOK("http://www.homeaway.com/vacation-rental/p100000", StringFormat.JSON));
		
		testSuite.add(new MetadataBadRequest("abc", StringFormat.XML));
		
		testSuite.add(new MetadataNotFound("http://www.abc.com/xyz", StringFormat.JSON));
		
		testSuite.add(new MMDStatusOK("http://www.abc.com/xyz.icom", StringFormat.XML, ParamType.URL));
		
		testSuite.add(new MMDStatusOK("http://www.amazon.com/gp/product/B0050SYS5A/", StringFormat.JSON, ParamType.URL));
		
		testSuite.add(new MMDStatusOK("acm_portal", StringFormat.XML, ParamType.NAME));
		
		testSuite.add(new MMDStatusOK("hotel", StringFormat.JSON, ParamType.NAME));
		
		testSuite.add(new MMDBadRequest("xyz", StringFormat.JSON, ParamType.URL));
		
		testSuite.add(new MMDNotFound("abcd", StringFormat.XML, ParamType.NAME));
	}

	private void runTestCases()
	{
		System.out.println("***** Executing " + testSuite.size() + " Test Cases ******** ");
		System.out.println();

		int i = 0;
		int fail = 0;

		for (BasicTest testCase : testSuite)
		{
			System.out
					.println("--------------------------------------------------------------------------------------------------------------------------------------------------");
			System.out.println("Test Case " + ++i + " : " + testCase.getClass().getCanonicalName());
			System.out
					.println("--------------------------------------------------------------------------------------------------------------------------------------------------");
			if (!testCase.doTest())
				fail++;
		}
		System.out.println();
		System.out.println("***** End: " + fail + " of " + i + " tests failed ********");
	}

	public static void main(String[] args)
	{
		TestRunner testRunner = new TestRunner();
		testRunner.runTestCases();
	}
}
