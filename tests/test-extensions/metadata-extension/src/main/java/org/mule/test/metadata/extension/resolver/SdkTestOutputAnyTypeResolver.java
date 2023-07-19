/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.resolver;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.sdk.api.metadata.MetadataContext;
import org.mule.sdk.api.metadata.resolving.OutputTypeResolver;

public class SdkTestOutputAnyTypeResolver implements OutputTypeResolver<String> {

  public static final String TEST_OUTPUT_ANY_TYPE_RESOLVER = "SdkTestOutputAnyTypeResolver";
  public static final String METADATA_EXTENSION_RESOLVER = "MetadataExtensionResolver";

  @Override
  public MetadataType getOutputType(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    return BaseTypeBuilder.create(JAVA).anyType().build();
  }

  @Override
  public String getCategoryName() {
    return METADATA_EXTENSION_RESOLVER;
  }

  @Override
  public String getResolverName() {
    return TEST_OUTPUT_ANY_TYPE_RESOLVER;
  }
}
