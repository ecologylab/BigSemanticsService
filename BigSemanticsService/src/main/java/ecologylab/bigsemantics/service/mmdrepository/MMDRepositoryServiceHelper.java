package ecologylab.bigsemantics.service.mmdrepository;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import ecologylab.bigsemantics.metametadata.MetaMetadataRepository;
import ecologylab.bigsemantics.service.SemanticsServiceErrorMessages;
import ecologylab.bigsemantics.service.SemanticsServiceScope;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * Helper class for mmdrepository root resources
 * 
 * @author ajit
 * 
 */

public class MMDRepositoryServiceHelper {
	
	static SemanticsServiceScope			semanticsServiceScope;
	
	static 
	{
		semanticsServiceScope = SemanticsServiceScope.get();
	}
	
	public static Response getMmdRepository(StringFormat format)
	{
		MetaMetadataRepository mmdRepository = semanticsServiceScope.getMetaMetadataRepository();
		if (mmdRepository != null)
		{
			try
			{
				String responseBody = SimplTypesScope.serialize(mmdRepository, format).toString();
				return Response.status(Status.OK).entity(responseBody).build();
			}
			catch (SIMPLTranslationException e)
			{
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(SemanticsServiceErrorMessages.INTERNAL_ERROR).type(MediaType.TEXT_PLAIN).build();
			}
		}
		else
			return Response.status(Status.NOT_FOUND).entity(SemanticsServiceErrorMessages.METAMETADATA_NOT_FOUND)
						.type(MediaType.TEXT_PLAIN).build();
	}
}
