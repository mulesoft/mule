package org.mule.test.metadata.extension.resolver;

import static org.mule.metadata.api.model.MetadataFormat.JSON;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.resolving.InputStaticTypeResolver;

public class AnyJsonTypeStaticResolver extends InputStaticTypeResolver {

  @Override
  public MetadataType getStaticMetadata() {
    return BaseTypeBuilder.create(JSON).anyType().build();
  }
}
