/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
