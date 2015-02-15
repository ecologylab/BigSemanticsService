package ecologylab.bigsemantics.dpool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.annotations.Hint;
import ecologylab.serialization.annotations.simpl_collection;
import ecologylab.serialization.annotations.simpl_composite;
import ecologylab.serialization.annotations.simpl_hints;
import ecologylab.serialization.annotations.simpl_scalar;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * 
 * @author quyin
 */
public class RemoteCurlDownloaderList
{

  @simpl_composite
  private RemoteCurlDownloader       defaultConfig;

  @simpl_collection("downloader")
  private List<RemoteCurlDownloader> downloaders;

  /**
   * This number is not maintained automatically. You need to call the getter to calculate it.
   */
  @simpl_scalar
  @simpl_hints(Hint.XML_LEAF)
  private int                        numDownloadersAlive;

  public RemoteCurlDownloader getDefaultConfig()
  {
    return defaultConfig;
  }

  public List<RemoteCurlDownloader> getDownloaders()
  {
    return downloaders;
  }

  public synchronized void addDownloader(RemoteCurlDownloader downloader)
  {
    if (downloaders == null)
    {
      downloaders = new ArrayList<RemoteCurlDownloader>();
    }
    downloaders.add(downloader);
  }

  public int getNumDownloadersAlive()
  {
    numDownloadersAlive = downloaders.size();
    return numDownloadersAlive;
  }

  public static void main(String[] args)
      throws SIMPLTranslationException, FileNotFoundException, IOException
  {
    RemoteCurlDownloaderList list = new RemoteCurlDownloaderList();
    list.defaultConfig = new RemoteCurlDownloader("localhost", 4);
    list.defaultConfig.setUser("user");
    list.downloaders = new ArrayList<RemoteCurlDownloader>();
    list.downloaders.add(new RemoteCurlDownloader("testhost", 5));
    String json = SimplTypesScope.serialize(list, StringFormat.JSON).toString();
    System.out.println(json);
  }

}
