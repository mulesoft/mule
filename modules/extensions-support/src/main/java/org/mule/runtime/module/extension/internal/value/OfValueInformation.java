/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.value;

import org.mule.sdk.api.annotation.binding.Binding;
import org.mule.sdk.api.annotation.values.OfValues;

/**
 * Encapsulates the information af the {@link OfValues} annotation or the
 * {@link org.mule.runtime.extension.api.annotation.values.OfValues} annotation.
 *
 * @since 4.4.0
 */
public class OfValueInformation {

  private final Class<?> value;
  private final boolean isOpen;
  private final Binding[] bindings;
  private final boolean fromLegacyAnnotation;

  public OfValueInformation(Class<?> clazz, boolean isOpen, Binding[] bindings, boolean fromLegacyAnnotation) {
    this.value = clazz;
    this.isOpen = isOpen;
    this.bindings = bindings;
    this.fromLegacyAnnotation = fromLegacyAnnotation;
  }

  public Class<?> getValue() {
    return value;
  }

  public boolean isOpen() {
    return isOpen;
  }

  public Binding[] getBindings() {
    return bindings;
  }

  public boolean isFromLegacyAnnotation() {
    return fromLegacyAnnotation;
  }
}
