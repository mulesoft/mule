/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.component;

import static org.mule.runtime.api.meta.AbstractAnnotatedObject.ROOT_CONTAINER_NAME_KEY;
import org.mule.runtime.api.meta.AnnotatedObject;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * This interface holds the keys used internally by the runtime for the annotations added to the
 * {@link org.mule.runtime.api.meta.AnnotatedObject}.
 */
public interface ComponentAnnotations {

  QName ANNOTATION_NAME = new QName("config", "componentIdentifier");
  QName ANNOTATION_PARAMETERS = new QName("config", "componentParameters");

  /**
   * Updates the {@link AnnotatedObject} root container name.
   * 
   * @param rootContainerName the root container name of the object.
   * @param annotatedObject the {@link AnnotatedObject} to update.
   */
  static void updateRootContainerName(String rootContainerName, AnnotatedObject annotatedObject) {
    Map<QName, Object> previousAnnotations = new HashMap<>(annotatedObject.getAnnotations());
    previousAnnotations.put(ROOT_CONTAINER_NAME_KEY, rootContainerName);
    annotatedObject.setAnnotations(previousAnnotations);
  }

}
