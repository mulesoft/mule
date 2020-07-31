package org.mule.tooling.extensions.metadata.internal.metadata;

import static java.util.Collections.emptySet;
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
import org.mule.tooling.extensions.metadata.internal.connection.TstExtensionClient;

import java.util.Collections;
import java.util.Set;

public class ConfigLessMetadataResolver implements TypeKeysResolver, OutputTypeResolver<String> {

  private static final String NAME = ConfigLessMetadataResolver.class.getSimpleName();

  @Override
  public MetadataType getOutputType(MetadataContext metadataContext, String s) throws MetadataResolvingException, ConnectionException {
    return BaseTypeBuilder.create(JAVA).stringType().build();
  }

  @Override
  public Set<MetadataKey> getKeys(MetadataContext metadataContext) throws MetadataResolvingException, ConnectionException {
    return metadataContext.<TstExtensionClient>getConnection()
            .map(TstExtensionClient::getName)
            .map(cn -> MetadataKeyBuilder.newKey(cn).build())
            .map(Collections::singleton)
            .orElse(emptySet());
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
