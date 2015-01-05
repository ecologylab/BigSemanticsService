package ecologylab.bigsemantics.downloaderpool;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * Util methods.
 * 
 * @author quyin
 */
public class DPoolUtils
{

  static Logger logger = LoggerFactory.getLogger(DPoolUtils.class);

  /**
   * Sleep for the given time in milliseconds. The sleep can be interrupted but it won't throw
   * exceptions.
   * 
   * @param millisec
   */
  public static void sleep(long millisec)
  {
    try
    {
      Thread.sleep(millisec);
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
  }

  private static HashFunction hf  = Hashing.sha256();

  private static BaseEncoding enc = BaseEncoding.base64Url();

  public static byte[] hashToBytes(String s)
  {
    return hf.hashString(s, Charsets.UTF_8).asBytes();
  }

  public static String base64urlEncode(byte[] bytes)
  {
    return enc.encode(bytes);
  }

  public static <K, V> boolean putNonEmpty(Map<K, V> map, K key, V value)
  {
    if (map == null || key == null || value == null)
      return false;
    if (value instanceof String && ((String) value).isEmpty())
      return false;
    map.put(key, value);
    return true;
  }

  public static String serialize(Object obj, StringFormat fmt)
  {
    if (obj == null)
      return null;

    try
    {
      return SimplTypesScope.serialize(obj, fmt).toString();
    }
    catch (SIMPLTranslationException e)
    {
      logger.error("Exception during serializing " + obj, e);
    }

    return null;
  }

  public static Object deserialize(CharSequence content, SimplTypesScope scope, StringFormat fmt)
  {
    if (content == null)
      return null;

    try
    {
      return scope.deserialize(content, fmt);
    }
    catch (SIMPLTranslationException e)
    {
      logger.error("Exception during deserializing:\n" + content, e);
    }

    return null;
  }

}
