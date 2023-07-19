/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.component;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;

import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Marker interface that tells that an object is created internally and not from an xml configuration.
 */
@NoImplement
public interface InternalComponent extends Component {

  @Override
  default Object getAnnotation(QName name) {
    return null;
  }

  @Override
  default Map<QName, Object> getAnnotations() {
    return null;
  }

  @Override
  default void setAnnotations(Map<QName, Object> annotations) {
    // Nothing to do
  }

  @Override
  default Location getRootContainerLocation() {
    return null;
  }

  @Override
  default ComponentLocation getLocation() {
    return null;
  }
}
