/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.resolver;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.resolving.InputStaticTypeResolver;

public class JsonInputStaticTypeResolver extends InputStaticTypeResolver {

  @Override
  public MetadataType getStaticMetadata() {
    ObjectTypeBuilder object = BaseTypeBuilder.create(MetadataFormat.JSON).objectType();
    object.id("json-object");
    object.addField().key("name").value().stringType();
    object.addField().key("last-name").value().stringType();
    return object.build();
  }
}
