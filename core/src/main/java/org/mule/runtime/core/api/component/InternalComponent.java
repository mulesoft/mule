/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.component;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;

import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Marker interface that tells that an object is created internally and not from an xml configuration.
 */
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
