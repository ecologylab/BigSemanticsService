/**
 * 
 */
package ecologylab.bigsemantics.service;

/**
 * Error messages to be returned by semantic service
 * 
 * @author ajit
 */
public interface SemanticServiceErrorMessages
{

  // 400
  final String BAD_REQUEST            = "Invalid parameters.";

  // 404
  final String METADATA_NOT_FOUND     = "Metadata not found for the requested URL.";

  // 404
  final String METAMETADATA_NOT_FOUND = "Meta-Metadata not found for the requested URL.";

  // 500
  final String INTERNAL_ERROR         = "Error occurred during handling the request. Try again later.";

  // 503
  final String SERVICE_UNAVAILABLE    = "The BigSemantics service is currently unavailable. Contact administrator.";

  // 504
  final String CONNECTION_TIMEOUT     = "Connection to the source resource timed out. Try again later.";

}
