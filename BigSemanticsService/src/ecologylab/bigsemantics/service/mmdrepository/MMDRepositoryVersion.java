package ecologylab.bigsemantics.service.mmdrepository;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ecologylab.bigsemantics.namesandnums.SemanticsAssetVersions;

/**
 * mmdrepository version root resource
 * 
 * @author ajit
 * 
 */

@Path("/mmdrepository.version")
@Component
@Scope("singleton")
public class MMDRepositoryVersion {

	@GET
	@Produces("text/plain")
	public Response getMmdRepositoryVersion()
	{
		return Response.status(Status.OK).
				entity((new Float(SemanticsAssetVersions.METAMETADATA_ASSET_VERSION).toString())).build();
	}
}
