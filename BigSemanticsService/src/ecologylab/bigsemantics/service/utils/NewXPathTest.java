package ecologylab.bigsemantics.service.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ecologylab.bigsemantics.cyberneko.CybernekoWrapper;
import ecologylab.bigsemantics.html.dom.IDOMProvider;
import ecologylab.bigsemantics.httpclient.HttpClientFactory;

public class NewXPathTest
{

  public static final String AGENT_CHROME      = "Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1667.0 Safari/537.36";

  public static final String AGENT_GOOGLE_BOT  = "Googlebot/2.1 (+http://www.google.com/bot.html)";

  IDOMProvider               domProvider       = new CybernekoWrapper();

  HttpClientFactory          httpClientFactory = new HttpClientFactory();

  XPath                      xp                = XPathFactory.newInstance().newXPath();

  public void testXPath(String agent, String url, String[] xpaths)
  {
    AbstractHttpClient client = httpClientFactory.get(agent);
    HttpGet get = new HttpGet(url);
    InputStream contentStream = null;
    try
    {
      HttpResponse resp = client.execute(get);
      HttpEntity entity = resp.getEntity();
      contentStream = entity.getContent();
    }
    catch (ClientProtocolException e)
    {
      e.printStackTrace();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }

    Document doc;
    try
    {
      doc = domProvider.parseDOM(contentStream, null);
      eval(doc, xpaths, 0, 0);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    catch (XPathExpressionException e)
    {
      e.printStackTrace();
    }
  }

  private void eval(Node node, String[] xpaths, int ipath, int inode)
      throws XPathExpressionException
  {
    String padding = "";
    for (int j = 0; j < ipath; ++j)
    {
      padding += "  ";
    }

    if (xpaths.length == ipath)
    {
      printNode(padding, node);
      return;
    }

    if (node != null)
    {
      String xpath = xpaths[ipath];
      if (xpath != null)
      {
        xpath = xpath.replaceAll("\\$i", String.valueOf(inode + 1));
        if (xpath.startsWith("c;"))
        {
          xpath = xpath.substring(2);
          NodeList nodes = (NodeList) xp.evaluate(xpath, node, XPathConstants.NODESET);
          print(padding, "List: node x" + nodes.getLength() + "\n");
          for (int j = 0; j < nodes.getLength(); ++j)
          {
            Node cnode = nodes.item(j);
            eval(cnode, xpaths, ipath + 1, j);
          }
        }
        else
        {
          Node cnode = (Node) xp.evaluate(xpath, node, XPathConstants.NODE);
          print(padding, "Node: " + cnode.getNodeName() + "\n");
          eval(cnode, xpaths, ipath + 1, 0);
        }
      }
    }
  }

  private void printNode(String padding, Node node)
  {
    if (padding == null)
    {
      padding = "";
    }

    if (node == null)
    {
      print(padding, "<null>\n");
      return;
    }
    print(padding, "Node name: " + node.getNodeName() + "\n");
    print(padding, "Node content: " + node.getTextContent() + "\n");
  }

  private void print(String padding, String msg)
  {
    if (padding == null)
    {
      padding = "";
    }
    System.out.print(padding);
    System.out.print(msg);
  }

  public static void main(String[] args) throws IOException
  {
    NewXPathTest xpathTest = new NewXPathTest();

    if (args.length < 3)
    {
      System.err.println("args: <agent-name> <url> <xpaths>");
      System.err.println("  where agent-name can be chrome/gbot/none");
      System.err.println("  <xpaths>: a list of xpaths. used hierarchically."
                         + " start with 'c;' for collections.");
      System.exit(-1);
    }

    String agentName = args[0];
    String agent = null;
    if (agentName == null || agentName.length() == 0 || "chrome".equals(agentName))
    {
      agent = AGENT_CHROME;
    }
    else if ("gbot".equals(agentName))
    {
      agent = AGENT_GOOGLE_BOT;
    }
    else
    {
      agent = "";
    }
    String url = args[1];
    String[] xpaths = Arrays.copyOfRange(args, 2, args.length);
    xpathTest.testXPath(agent, url, xpaths);
  }

}
