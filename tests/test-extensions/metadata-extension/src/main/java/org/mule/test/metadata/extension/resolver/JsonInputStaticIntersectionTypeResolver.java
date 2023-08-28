/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.resolver;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.IntersectionTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.sdk.api.metadata.resolving.InputStaticTypeResolver;

public class JsonInputStaticIntersectionTypeResolver extends InputStaticTypeResolver {

  @Override
  public MetadataType getStaticMetadata() {
    ObjectTypeBuilder object = BaseTypeBuilder.create(MetadataFormat.JSON).objectType();
    object.id("json-object1");
    object.addField().key("age").value().numberType();
    object.addField().key("dni").value().numberType();

    IntersectionTypeBuilder intersectionTypeBuilder = BaseTypeBuilder.create(MetadataFormat.JSON).intersectionType();
    intersectionTypeBuilder.of(object);
    return intersectionTypeBuilder.build();
  }
}
