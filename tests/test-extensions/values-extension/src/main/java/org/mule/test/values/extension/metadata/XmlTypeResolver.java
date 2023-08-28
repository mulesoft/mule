/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.metadata;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;

import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.resolving.InputStaticTypeResolver;

public class XmlTypeResolver extends InputStaticTypeResolver {

  @Override
  public MetadataType getStaticMetadata() {
    return create(MetadataFormat.XML).anyType().build();
  }

}
