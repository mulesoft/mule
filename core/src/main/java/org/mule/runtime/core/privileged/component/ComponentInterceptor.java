/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.component;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;

import java.util.Map;

import javax.xml.namespace.QName;

public final class ComponentInterceptor extends AbstractComponent {

  @Override
  public Object getAnnotation(QName qName) {
    return super.getAnnotation(qName);
  }

  @Override
  public Map<QName, Object> getAnnotations() {
    return super.getAnnotations();
  }

  @Override
  public void setAnnotations(Map<QName, Object> newAnnotations) {
    super.setAnnotations(newAnnotations);
  }

  @Override
  public ComponentLocation getLocation() {
    return super.getLocation();
  }

  @Override
  public Location getRootContainerLocation() {
    return super.getRootContainerLocation();
  }

  @Override
  public ComponentIdentifier getIdentifier() {
    return super.getIdentifier();
  }

  @Override
  public String getRepresentation() {
    return super.getRepresentation();
  }

  @Override
  public String getDslSource() {
    return super.getDslSource();
  }
}
