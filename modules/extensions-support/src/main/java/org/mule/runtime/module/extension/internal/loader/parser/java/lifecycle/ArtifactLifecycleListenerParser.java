/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.lifecycle;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.sdk.api.annotation.OnArtifactLifecycle;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.util.Optional;

/**
 * Helper class for parsing an optional {@link ArtifactLifecycleListener} out of the annotations of an {@link ExtensionElement}.
 */
public class ArtifactLifecycleListenerParser {

  public Optional<ArtifactLifecycleListener> getArtifactLifecycleListener(ExtensionElement extensionElement) {
    return extensionElement.getValueFromAnnotation(OnArtifactLifecycle.class)
        .flatMap(this::instantiateArtifactLifecycleListener);
  }

  private Optional<ArtifactLifecycleListener> instantiateArtifactLifecycleListener(AnnotationValueFetcher<OnArtifactLifecycle> annotationValueFetcher) {
    return annotationValueFetcher.getClassValue(OnArtifactLifecycle::value)
        .getDeclaringClass()
        .map(this::instantiateArtifactLifecycleListener);
  }

  private ArtifactLifecycleListener instantiateArtifactLifecycleListener(Class<?> clazz) {
    checkArgument(clazz != null, "ArtifactLifecycleListener type cannot be null");
    try {
      return (ArtifactLifecycleListener) ClassUtils.instantiateClass(clazz);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create ArtifactLifecycleListener of type "
          + clazz.getName()),
                                     e);
    }
  }
}
