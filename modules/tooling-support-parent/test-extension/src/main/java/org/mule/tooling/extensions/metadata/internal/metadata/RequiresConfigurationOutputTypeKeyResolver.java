package org.mule.tooling.extensions.metadata.internal.metadata;

import static java.util.Collections.emptySet;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;

import java.util.Set;

public class RequiresConfigurationOutputTypeKeyResolver implements TypeKeysResolver, OutputTypeResolver<String> {

  public static final String CONFIGURATION_IS_NOT_PRESENT_A_MESSAGE_FROM_RESOLVER = "Configuration is not present, a message from resolver";

  @Override
  public MetadataType getOutputType(MetadataContext metadataContext, String key) throws MetadataResolvingException, ConnectionException {
    metadataContext.getConfig().orElseThrow(() -> new MetadataResolvingException(CONFIGURATION_IS_NOT_PRESENT_A_MESSAGE_FROM_RESOLVER, UNKNOWN));
    return BaseTypeBuilder.create(JAVA).stringType().defaultValue(key).build();
  }

  @Override
  public Set<MetadataKey> getKeys(MetadataContext metadataContext) throws MetadataResolvingException, ConnectionException {
    metadataContext.getConfig().orElseThrow(() -> new MetadataResolvingException(CONFIGURATION_IS_NOT_PRESENT_A_MESSAGE_FROM_RESOLVER, UNKNOWN));
    return emptySet();
  }

  @Override
  public String getResolverName() {
    return this.getClass().getName();
  }

  @Override
  public String getCategoryName() {
    return this.getResolverName();
  }

}
