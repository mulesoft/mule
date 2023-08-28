/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.resolving.AttributesStaticTypeResolver;
import org.mule.test.metadata.extension.resolver.JsonInputStaticTypeResolver;

public class JsonStaticAttributesTypeResolver extends AttributesStaticTypeResolver {

  @Override
  public MetadataType getStaticMetadata() {
    return new JsonInputStaticTypeResolver().getStaticMetadata();
  }
}
