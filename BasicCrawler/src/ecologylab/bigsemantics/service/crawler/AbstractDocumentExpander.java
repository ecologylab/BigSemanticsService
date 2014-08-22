package ecologylab.bigsemantics.service.crawler;

import java.util.Collection;

import ecologylab.bigsemantics.metadata.builtins.CompoundDocument;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metametadata.MetaMetadata;
import ecologylab.bigsemantics.metametadata.MetaMetadataCollectionField;
import ecologylab.bigsemantics.metametadata.MetaMetadataCompositeField;
import ecologylab.bigsemantics.metametadata.MetaMetadataField;
import ecologylab.net.ParsedURL;

/**
 * 
 * @author quyin
 */
public class AbstractDocumentExpander implements DocumentExpander
{

  @Override
  public void expand(AbstractDocumentCrawler crawler, Document doc)
  {
    MetaMetadata mmd = (MetaMetadata) doc.getMetaMetadata();
    if (mmd == null)
      throw new NullPointerException("Document to expand does not have meta-metadata associated.");

    for (MetaMetadataField field : mmd.getChildrenMap())
    {
      if (field instanceof MetaMetadataCompositeField)
      {
        Object value = field.getMetadataFieldDescriptor().getValue(doc);
        queueLinkedDocIfApplicable(crawler, value);
      }
      else if (field instanceof MetaMetadataCollectionField)
      {
        MetaMetadataCollectionField collectionField = (MetaMetadataCollectionField) field;
        if (!collectionField.isCollectionOfScalars())
        {
          Collection<?> collection = field.getMetadataFieldDescriptor().getCollection(doc);
          if (collection != null)
          {
            for (Object item : collection)
              queueLinkedDocIfApplicable(crawler, item);
          }
        }
      }
    }
  }

  protected void queueLinkedDocIfApplicable(ResourceCrawler<Document> crawler, Object linkedDoc)
  {
    if (linkedDoc != null
        && linkedDoc instanceof Document
        && !linkedDoc.getClass().equals(Document.class)
        && !linkedDoc.getClass().equals(CompoundDocument.class))
    {
      ParsedURL linkedDocLoc = ((Document) linkedDoc).getLocation();
      if (linkedDocLoc != null)
        crawler.queue(linkedDocLoc.toString());
    }
  }

}
