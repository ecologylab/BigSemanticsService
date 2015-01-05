package ecologylab.bigsemantics.service;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.StringFormat;

public class ServiceUtils
{

  static Logger logger = LoggerFactory.getLogger(ServiceUtils.class);

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

  public static String urlencode(String content)
  {
    try
    {
      return URLEncoder.encode(content, "UTF-8");
    }
    catch (UnsupportedEncodingException e)
    {
      logger.warn("Cannot encode for URL: {}", content);
      return null;
    }
  }

}
