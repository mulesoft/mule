/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.resolver;

import static org.mule.metadata.api.model.MetadataFormat.CSV;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.resolving.InputStaticTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputStaticTypeResolver;

public class CsvInputStaticTypeResolver extends OutputStaticTypeResolver {

  @Override
  public MetadataType getStaticMetadata() {
    return BaseTypeBuilder.create(CSV).objectType().id("csv-object").build();
  }
}
