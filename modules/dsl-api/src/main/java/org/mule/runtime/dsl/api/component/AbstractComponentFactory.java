/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;

/**
 * Basic implementation of {@link ComponentFactory} that handles all annotation related behavior including
 * {@link ObjectFactory#getObject()}.
 *
 * @param <T> the type of the object to be created, which should be an {@link Component}.
 */
public abstract class AbstractComponentFactory<T> extends AbstractComponent implements ComponentFactory<T> {

  /**
   * Method to be implemented instead of {@link ObjectFactory#getObject()}.
   *
   * @return the domain object
   * @throws Exception if any failure occurs building the object
   */
  public abstract T doGetObject() throws Exception;

  @Override
  public T getObject() throws Exception {
    T annotatedInstance = doGetObject();
    ((Component) annotatedInstance).setAnnotations(getAnnotations());
    return annotatedInstance;
  }
}
