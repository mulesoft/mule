/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.annotations;

import org.mule.metadata.api.annotation.MarkerAnnotation;
import org.mule.metadata.api.annotation.TypeAnnotation;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;

/**
 * A {@link MarkerAnnotation} to signal that a {@link MetadataType} is a custom built static type.
 *
 * @since 1.1
 */
public class CustomDefinedStaticTypeAnnotation extends MarkerAnnotation {

  public static final String NAME = "customStaticType";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean isPublic() {
    return false;
  }
}
