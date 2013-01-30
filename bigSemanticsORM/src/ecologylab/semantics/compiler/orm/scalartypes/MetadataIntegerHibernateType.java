package ecologylab.semantics.compiler.orm.scalartypes;

import org.hibernate.type.IntegerType;

import ecologylab.semantics.metadata.scalar.MetadataInteger;

public class MetadataIntegerHibernateType extends MetadataScalarHibernateType
{

	public MetadataIntegerHibernateType()
	{
		super(MetadataInteger.class, IntegerType.INSTANCE);
	}

}
