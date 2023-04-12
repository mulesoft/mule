/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;

import java.util.Map;

import javax.xml.namespace.QName;

/**
 * A {@link Component} used for tracing when it cannot be identified.
 */
public class UnnamedComponent implements Component {

  private static final Component INSTANCE = new UnnamedComponent();

  public static Component getUnnamedComponent() {
    return INSTANCE;
  }

  private UnnamedComponent() {}

  @Override
  public Object getAnnotation(QName qName) {
    return null;
  }

  @Override
  public Map<QName, Object> getAnnotations() {
    return emptyMap();
  }

  @Override
  public void setAnnotations(Map<QName, Object> map) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ComponentLocation getLocation() {
    return null;
  }

  @Override
  public Location getRootContainerLocation() {
    return null;
  }
}
