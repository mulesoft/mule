package org.mule.tooling.extensions.metadata.internal.metadata;

import static java.util.Collections.singleton;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;

import java.util.Set;

public class ConfigLessConnectionLessMetadataResolver implements TypeKeysResolver, OutputTypeResolver<String> {

  private static final String NAME = ConfigLessConnectionLessMetadataResolver.class.getSimpleName();

  @Override
  public MetadataType getOutputType(MetadataContext metadataContext, String s) throws MetadataResolvingException, ConnectionException {
    return BaseTypeBuilder.create(JAVA).stringType().build();
  }

  @Override
  public Set<MetadataKey> getKeys(MetadataContext metadataContext) throws MetadataResolvingException, ConnectionException {
    return singleton(MetadataKeyBuilder.newKey(NAME).build());
  }

  @Override
  public String getResolverName() {
    return NAME;
  }

  @Override
  public String getCategoryName() {
    return NAME;
  }
}
