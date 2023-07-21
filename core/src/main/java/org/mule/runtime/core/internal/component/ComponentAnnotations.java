/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.component;

import static org.mule.runtime.api.component.AbstractComponent.ROOT_CONTAINER_NAME_KEY;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * This interface holds the keys used internally by the runtime for the annotations added to the {@link Component}.
 */
public interface ComponentAnnotations {

  QName ANNOTATION_NAME = AbstractComponent.ANNOTATION_NAME;
  QName ANNOTATION_PARAMETERS = new QName("config", "componentParameters");
  QName ANNOTATION_COMPONENT_CONFIG = new QName("config", "componentConfiguration");

  /**
   * Updates the {@link Component} root container name.
   *
   * @param rootContainerName the root container name of the object.
   * @param component         the {@link Component} to update.
   */
  static void updateRootContainerName(String rootContainerName, Component component) {
    Map<QName, Object> previousAnnotations = new HashMap<>(component.getAnnotations());
    previousAnnotations.put(ROOT_CONTAINER_NAME_KEY, rootContainerName);
    component.setAnnotations(previousAnnotations);
  }

}
