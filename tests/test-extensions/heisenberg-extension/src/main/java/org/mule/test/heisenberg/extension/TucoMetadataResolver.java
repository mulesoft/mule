/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.resolving.OutputStaticTypeResolver;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;

public class TucoMetadataResolver extends OutputStaticTypeResolver {

  private static final AnyType ANY_TYPE = BaseTypeBuilder.create(JAVA).anyType().build();

  @Override
  public String getCategoryName() {
    return "TUCO";
  }

  @Override
  public MetadataType getStaticMetadata() {
    return ANY_TYPE;
  }

}
