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

class MetadataTypeModelAdapter implements HasStereotypeModel {

  private final MetadataType type;
  private final StereotypeModel stereotype;

  public MetadataTypeModelAdapter(MetadataType type) {
    this.type = type;
    this.stereotype = type.getAnnotation(StereotypeTypeAnnotation.class).get().getAllowedStereotypes().get(0);
  }

  @Override
  public StereotypeModel getStereotype() {
    return stereotype;
  }

}
