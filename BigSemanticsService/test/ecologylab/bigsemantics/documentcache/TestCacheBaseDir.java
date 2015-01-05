package ecologylab.bigsemantics.documentcache;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class TestCacheBaseDir
{

  @Test
  public void testExpandHomeDir()
  {
    String path = "$HOME/bigsemantics-service/cache";
    String result = DiskPersistentDocumentCache.expandHomeDir(path);
    File dir = new File(result);
    System.out.println(dir);
    assertEquals("cache", dir.getName());
    assertEquals("bigsemantics-service", dir.getParentFile().getName());
  }

}
