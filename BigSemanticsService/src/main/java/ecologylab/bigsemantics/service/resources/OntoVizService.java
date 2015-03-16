package ecologylab.bigsemantics.service.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.metametadata.MetaMetadataRepository;
import ecologylab.bigsemantics.metametadata.RepositoryOrderingByGeneration;
import ecologylab.bigsemantics.metametadata.RepositoryOrderingByGeneration.TreeNode;
import ecologylab.bigsemantics.service.SemanticsServiceScope;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.annotations.simpl_collection;
import ecologylab.serialization.annotations.simpl_nowrap;
import ecologylab.serialization.annotations.simpl_scalar;
import ecologylab.serialization.formatenums.StringFormat;

@Path("/onto")
public class OntoVizService
{

  static Logger         logger                 = LoggerFactory.getLogger(OntoVizService.class);

  static String         cachedTreeJson;

  static String         cachedTableJson;

  @Inject
  SemanticsServiceScope semanticsServiceScope;

  @QueryParam("retain_subdomain")
  String                retainSubdomain;

  String                defaultRetainSubdomain = "psu.edu,google.com,";

  @QueryParam("display_domains")
  String                displayDomains;

  String                defaultDisplayDomains  = "airbnb.com,"
                                                 + "amazon.co.uk,"
                                                 + "amazon.com,"
                                                 + "apple.com,"
                                                 + "ebay.com,"
                                                 + "etsy.com,"
                                                 + "hilton.com,"
                                                 + "homeaway.com,"
                                                 + "newegg.com,"
                                                 + "overstock.com,"
                                                 + "target.com,"
                                                 + "tigerdirect.com,"
                                                 + "tripadvisor.com,"
                                                 + "urbanspoon.com,"
                                                 + "warlmart.com,"
                                                 + "yelp.com,";

  @Path("tree.json")
  @GET
  @Produces("application/json")
  public Response getTree()
  {
    if (cachedTreeJson == null)
    {
      synchronized (OntoVizService.class)
      {
        if (cachedTreeJson == null)
        {
          MetaMetadataRepository repository = semanticsServiceScope.getMetaMetadataRepository();
          RepositoryOrderingByGeneration ordering = new RepositoryOrderingByGeneration();
          ordering.orderMetaMetadataForInheritance(repository.getMetaMetadataCollection());
          try
          {
            cachedTreeJson = SimplTypesScope.serialize(ordering.root, StringFormat.JSON).toString();
          }
          catch (SIMPLTranslationException e)
          {
            logger.error("Error serializing onto viz tree data", e);
          }
        }
      }
    }
    return Response.ok(cachedTreeJson).type("application/json").build();
  }

  static class DomainExample
  {

    @simpl_scalar
    String       domain;

    @simpl_collection("type")
    List<String> types;

    @simpl_collection("url")
    List<String> urls;

  }

  static class DomainExamples
  {

    @simpl_collection("example")
//    @simpl_nowrap
    List<DomainExample> examples;

  }

  @Path("table.json")
  @GET
  @Produces("application/json")
  public Response getTable()
  {
    if (cachedTableJson == null)
    {
      synchronized (OntoVizService.class)
      {
        if (cachedTableJson == null)
        {
          MetaMetadataRepository repository = semanticsServiceScope.getMetaMetadataRepository();
          RepositoryOrderingByGeneration ordering = new RepositoryOrderingByGeneration();
          ordering.orderMetaMetadataForInheritance(repository.getMetaMetadataCollection());
          Map<String, Map<String, List<String>>> result =
              new HashMap<String, Map<String, List<String>>>();
          traverseAndCollectExampleUrls(ordering.root, result);

          DomainExamples examples = new DomainExamples();
          examples.examples = new ArrayList<DomainExample>();
          for (String domain : result.keySet())
          {
            DomainExample example = new DomainExample();
            example.domain = domain;
            example.types = new ArrayList<String>();
            example.types.addAll(result.get(domain).keySet());
            example.urls = new ArrayList<String>();
            for (String type : example.types)
            {
              example.urls.addAll(result.get(domain).get(type));
            }
            examples.examples.add(example);
          }

          try
          {
            cachedTableJson = SimplTypesScope.serialize(examples, StringFormat.JSON).toString();
          }
          catch (SIMPLTranslationException e)
          {
            logger.error("Error serializing onto domain URL example data", e);
          }
        }
      }
    }
    return Response.ok(cachedTableJson).type("application/json").build();
  }

  private void traverseAndCollectExampleUrls(TreeNode node,
                                             Map<String, Map<String, List<String>>> result)
  {
    if (node != null)
    {
      String name = node.getName();
      List<String> urls = node.getAllExampleUrls();
      if (urls != null && urls.size() > 0)
      {
        String subdomain = ParsedURL.getAbsolute(urls.get(0)).host();
        String domain = getTopLevelDomain(subdomain);

        String d = domain;
        if (retainSubdomain().contains(d))
        {
          d = subdomain;
        }

        if (displayDomains().contains(d))
        {
          if (!result.containsKey(d))
          {
            result.put(d, new HashMap<String, List<String>>());
          }
          if (!result.get(d).containsKey(name))
          {
            result.get(d).put(name, new ArrayList<String>());
          }
          result.get(d).get(name).addAll(urls);
        }
      }

      if (node.getSubtypes() != null)
      {
        for (TreeNode subtype : node.getSubtypes())
        {
          traverseAndCollectExampleUrls(subtype, result);
        }
      }
    }
  }

  public String retainSubdomain()
  {
    return retainSubdomain == null ? defaultRetainSubdomain : retainSubdomain;
  }

  public String displayDomains()
  {
    return displayDomains == null ? defaultDisplayDomains : displayDomains;
  }

  private String getTopLevelDomain(String subdomain)
  {
    int i = subdomain.length() - 1;
    int n = 2;
    while (i >= 0)
    {
      if (subdomain.charAt(i) == '.')
      {
        n--;
      }
      if (n == 0)
      {
        break;
      }
      i--;
    }

    if (n == 0)
    {
      return subdomain.substring(i + 1);
    }
    else
    {
      return subdomain;
    }
  }

}
