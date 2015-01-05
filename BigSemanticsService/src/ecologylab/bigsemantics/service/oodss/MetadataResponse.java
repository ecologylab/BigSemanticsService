/**
 * 
 */
package ecologylab.bigsemantics.service.oodss;

import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.namesandnums.SemanticsNames;
import ecologylab.collections.Scope;
import ecologylab.oodss.messages.ResponseMessage;
import ecologylab.serialization.annotations.simpl_composite;
import ecologylab.serialization.annotations.simpl_scope;

/**
 * response message containing downloaded document 
 * now moved to semantics project, retained for gsoc m1
 * 
 * @author ajit
 * 
 */

public class MetadataResponse extends ResponseMessage
{

	@simpl_scope(SemanticsNames.REPOSITORY_METADATA_TYPE_SCOPE)
	@simpl_composite
	Document	metadata;

	public MetadataResponse()
	{
	}

	public MetadataResponse(Document metadata)
	{
		this.metadata = metadata;
	}

	/*
	 * Called automatically by OODSS on client
	 */
	@Override
	public void processResponse(Scope appObjScope)
	{
		MetadataResponseListener responseListener = (MetadataResponseListener) appObjScope
				.get("RESPONSE_LISTENER");
		responseListener.setResponse(this.metadata);
	}

	@Override
	public boolean isOK()
	{
		return true;
	}
}
