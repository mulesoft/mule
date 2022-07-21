/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.component.value;

import static java.util.Collections.emptySet;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.component.value.ArrayValueDeclarer;
import org.mule.runtime.extension.api.component.value.MapValueDeclarer;
import org.mule.runtime.extension.api.component.value.ObjectValueDeclarer;
import org.mule.runtime.extension.api.component.value.ValueDeclarer;

import java.util.Collections;
import java.util.Set;

public class DefaultValueDeclarer implements ValueDeclarer {

  private final MetadataType type;
  private final Set<ModelProperty> modelProperties;

  public DefaultValueDeclarer(MetadataType type) {
    this(type, emptySet());
  }

  public DefaultValueDeclarer(MetadataType type, Set<ModelProperty> modelProperties) {
    this.type = type;
    this.modelProperties = modelProperties;
  }

  @Override
  public MapValueDeclarer asMapValue() {
    return null;
  }

  @Override
  public ObjectValueDeclarer asObjectValue() {
    return null;
  }

  @Override
  public ArrayValueDeclarer asArrayValue() {
    return null;
  }

  @Override
  public ValueDeclarer withValue(Object value) {
    return null;
  }
}
