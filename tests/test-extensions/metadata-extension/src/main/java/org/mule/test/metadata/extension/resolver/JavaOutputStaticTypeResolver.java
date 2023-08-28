/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.resolver;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.resolving.InputStaticTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputStaticTypeResolver;

public class JavaOutputStaticTypeResolver extends OutputStaticTypeResolver {

  @Override
  public MetadataType getStaticMetadata() {
    return BaseTypeBuilder.create(MetadataFormat.JAVA).objectType().id("custom-java").build();
  }
}
