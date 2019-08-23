/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.stereotype.HasStereotypeModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.declaration.type.annotation.StereotypeTypeAnnotation;

import java.util.Optional;

class MetadataTypeModelAdapter implements HasStereotypeModel {

  static Optional<MetadataTypeModelAdapter> createMetadataTypeModelAdapter(MetadataType type) {
    return type.getAnnotation(StereotypeTypeAnnotation.class)
        .flatMap(sta -> sta.getAllowedStereotypes().stream().findFirst())
        .map(st -> new MetadataTypeModelAdapter(type, st));
  }

  private final MetadataType type;
  private final StereotypeModel stereotype;

  private MetadataTypeModelAdapter(MetadataType type, StereotypeModel stereotype) {
    this.type = type;
    this.stereotype = stereotype;
  }

  @Override
  public StereotypeModel getStereotype() {
    return stereotype;
  }

}
