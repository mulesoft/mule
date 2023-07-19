/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.resolver;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.getMetadata;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

public class TestInputAndOutputResolverWithoutKeyResolverAndKeyIdParam
    implements InputTypeResolver<String>, OutputTypeResolver<String> {

  private static final String KEY_SHOULD_BE_EMPTY = "Metadata resolvers without Key Resolver should get a NullMetadataKey as Key";

  @Override
  public String getResolverName() {
    return "TestInputAndOutputResolverWithoutKeyResolverAndKeyIdParam";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String key) throws MetadataResolvingException {
    checkArgument(isBlank(key), KEY_SHOULD_BE_EMPTY);
    return getMetadata(PERSON);
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, String key) throws MetadataResolvingException {
    checkArgument(isBlank(key), KEY_SHOULD_BE_EMPTY);
    return getMetadata(PERSON);
  }

  @Override
  public String getCategoryName() {
    return "MetadataExtensionResolver";
  }
}
