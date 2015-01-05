/**
 * 
 */
package ecologylab.bigsemantics.service.oodss;

import ecologylab.oodss.messages.DefaultServicesTranslations;
import ecologylab.serialization.SimplTypesScope;

/**
 * translation scope used by OODSS server and client now moved to semantics project, retained for
 * gsoc m1
 * 
 * @author ajit
 * 
 */
public class SemanticsServiceMessageScope
{

  private static SimplTypesScope oodssTranslationScope = null;

  public synchronized static SimplTypesScope get()
  {
    if (oodssTranslationScope == null)
    {
      /*
       * get base translations with static accessor
       */
      SimplTypesScope baseServices = DefaultServicesTranslations.get();

      /*
       * Classes that must be translated by the translation scope in order for the server to
       * communicate w/ the client
       */
      Class[] lookupMetadataClasses = { MetadataRequest.class, MetadataResponse.class };

      /*
       * compose translations, to create the space inheriting the base translations
       */
      oodssTranslationScope = SimplTypesScope.get("SEMANTICS_SERVICE_METADATA_SERVER",
                                                  baseServices,
                                                  lookupMetadataClasses);
    }
    return oodssTranslationScope;
  }
  
}
