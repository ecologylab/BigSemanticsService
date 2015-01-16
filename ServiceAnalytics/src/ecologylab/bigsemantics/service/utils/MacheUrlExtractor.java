/**
 *
 */
package ecologylab.bigsemantics.service.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * utility class to extract clipping source urls from the maches. maches can be added one-by-one,
 * through a list, or a file.
 * 
 * @author ajit
 * 
 */

public class MacheUrlExtractor
{
	ArrayList<URL>	macheUrls						= new ArrayList<URL>();

	ArrayList<URL>	clippingSourceUrls	= new ArrayList<URL>();

	OutputStream		outputStream				= System.out;

	static String		userAgent						= "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.60 Safari/537.17";

	boolean					bExtracted;

	MacheUrlExtractor(URL macheUrl, OutputStream outputStream)
	{
		this(macheUrl);
		this.outputStream = outputStream;
	}

	MacheUrlExtractor(List<URL> macheUrls, OutputStream outputStream)
	{
		this(macheUrls);
		this.outputStream = outputStream;
	}

	MacheUrlExtractor(File macheUrlFile, OutputStream outputStream) throws IOException
	{
		this(macheUrlFile);
		this.outputStream = outputStream;
	}

	MacheUrlExtractor(URL macheUrl) // ParsedUrl?
	{
		macheUrls.add(macheUrl);
	}

	MacheUrlExtractor(List<URL> macheUrls)
	{
		for (URL macheUrl : macheUrls)
			macheUrls.add(macheUrl);
	}

	MacheUrlExtractor(File macheUrlFile) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(macheUrlFile));
		String macheUrl;
		while ((macheUrl = reader.readLine()) != null)
		{
			macheUrls.add(new URL(macheUrl));
		}
		if (reader != null)
			reader.close();
	}

	List<URL> getMacheSourceUrls()
	{
		bExtracted = true;
		if (macheUrls != null)
		{
			for (URL macheUrl : macheUrls)
			{
				try
				{
					// map json response to get clipping source_doc
					ObjectMapper m = new ObjectMapper();
					JsonNode rootNode = m.readValue(macheUrl, JsonNode.class);

					LinkedList<JsonNode> list = new LinkedList<JsonNode>();
					list.add(rootNode);

					JsonNode currentNode;
					while ((currentNode = list.poll()) != null)
					{
						JsonNode srcDocNode = currentNode.get("source_doc");
						if (srcDocNode != null)
						{
							JsonNode locNode = srcDocNode.get("location");
							if (locNode != null)
							{
								try
								{
									clippingSourceUrls.add(new URL(locNode.getTextValue()));
								}
								catch (MalformedURLException e)
								{
									System.err.println("malformed url: " + locNode.getTextValue());
									e.printStackTrace();
								}
							}
						}

						JsonNode outlinksNode = currentNode.get("outlinks");
						if (outlinksNode != null)
						{
							int arrIndex = 0;
							JsonNode arrNode;
							while ((arrNode = outlinksNode.get(arrIndex++)) != null)
							{
								JsonNode locNode = arrNode.get("location");
								if (locNode != null)
								{
									try
									{
										clippingSourceUrls.add(new URL(locNode.getTextValue()));
									}
									catch (MalformedURLException e)
									{
										System.err.println("malformed url: " + locNode.getTextValue());
										e.printStackTrace();
									}
								}
							}
						}

						Iterator<JsonNode> it = currentNode.getElements();
						while (it.hasNext())
						{
							JsonNode node = it.next();
							list.add(node);
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return clippingSourceUrls;
	}

	void outputMacheSourceUrls()
	{
		if (!bExtracted)
			getMacheSourceUrls();

		if (clippingSourceUrls != null)
		{
			PrintWriter pw = new PrintWriter(outputStream);
			for (URL clippingSourceUrl : clippingSourceUrls)
				pw.println(clippingSourceUrl);
			pw.close();
		}
	}

	public static void main(String[] args)
	{
		try
		{
			MacheUrlExtractor m = new MacheUrlExtractor(new File(
					"/home/ajit/ServiceTest/urls/macheURLs.lst"), new FileOutputStream(
					"/home/ajit/ServiceTest/urls/macheSourceURLs.lst"));
			m.outputMacheSourceUrls();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
