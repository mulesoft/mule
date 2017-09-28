/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.processor;

import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.privileged.processor.SecurityFilterMessageProcessor;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

/**
 * Object factory for custom {@link SecurityFilter}.
 *
 * @since 4.0
 */
public class CustomSecurityFilterObjectFactory extends AbstractComponentFactory<SecurityFilterMessageProcessor> {

  private final SecurityFilter filter;

  public CustomSecurityFilterObjectFactory(SecurityFilter filter) {
    this.filter = filter;
  }

  @Override
  public SecurityFilterMessageProcessor doGetObject() throws Exception {
    return new SecurityFilterMessageProcessor(getFilter());
  }

  public SecurityFilter getFilter() {
    return filter;
  }

}
