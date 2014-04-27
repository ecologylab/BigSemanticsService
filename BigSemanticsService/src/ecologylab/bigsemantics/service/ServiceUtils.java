package ecologylab.bigsemantics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

}
