/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension.metadata;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;

import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.resolving.InputStaticTypeResolver;

public class JsonTypeResolver extends InputStaticTypeResolver {

  @Override
  public MetadataType getStaticMetadata() {
    return create(MetadataFormat.JSON).anyType().build();
  }

}
