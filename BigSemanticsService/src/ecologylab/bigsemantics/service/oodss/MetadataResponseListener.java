/**
 * 
 */
package ecologylab.bigsemantics.service.oodss;

import ecologylab.bigsemantics.metadata.builtins.Document;

/**
 * listener interface for response message 
 * now moved to semantics project, retained for gsoc m1
 * 
 * @author ajit
 * 
 */

public interface MetadataResponseListener
{
	public void setResponse(Document metadata);
}
