package ecologylab.bigsemantics.downloaderpool;

import java.io.IOException;
import java.util.Scanner;

import ecologylab.bigsemantics.httpclient.HttpClientFactory;
import ecologylab.net.ParsedURL;


/**
 * A tool for inspecting the downloading result using Page.
 * 
 * @author quyin
 */
public class PageTool
{

  public static void main(String[] args) throws IOException
  {
    while (true)
    {
      System.out.print("Enter a URL to retrieve: ");
      Scanner scanner = new Scanner(System.in);
      String url = scanner.nextLine();
      if (url == null || url.length() == 0)
      {
        break;
      }
      
      if (!url.startsWith("http://") && !url.startsWith("https://"))
      {
        url = "http://" + url;
      }

      Page p = new Page("page-tool", ParsedURL.getAbsolute(url), null);
      p.clientFactory = new HttpClientFactory();
      p.siteTable = new SimpleSiteTable();
      p.performDownload();
      DownloaderResult result = p.getResult();
      
      System.out.println("\n\n\n------------------------------------------------------------\n");
      System.out.println("ID:          " + result.getTaskId());
      System.out.println("STATE:       " + result.getState());
      System.out.println("URL:         " + url);
      System.out.println("OTHER URLs:  " + result.getOtherLocations());
      System.out.println("STATUS CODE: " + result.getHttpRespCode());
      System.out.println("STATUS MSG:  " + result.getHttpRespMsg());
      System.out.println("MIME TYPE:   " + result.getMimeType());
      System.out.println("CHARSET:     " + result.getCharset());
      System.out.println("CONTENT:");
      System.out.println(result.getContent());
      System.out.println("\n------------------------------------------------------------\n\n\n");
    }
  }

}
