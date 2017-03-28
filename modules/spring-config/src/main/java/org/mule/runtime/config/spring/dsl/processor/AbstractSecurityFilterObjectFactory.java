/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.processor.SecurityFilterMessageProcessor;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;

/**
 * Abstract implementation of a {@link SecurityFilter} factory. Subclasses need only provide the actual filter implementation.
 *
 * @since 4.0
 */
public abstract class AbstractSecurityFilterObjectFactory<T extends SecurityFilter>
    extends AbstractAnnotatedObjectFactory<SecurityFilterMessageProcessor> {

  @Override
  public SecurityFilterMessageProcessor doGetObject() throws Exception {
    return new SecurityFilterMessageProcessor(getFilter());
  }

  public abstract T getFilter();

}
