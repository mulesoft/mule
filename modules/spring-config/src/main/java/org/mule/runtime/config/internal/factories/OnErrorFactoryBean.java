/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

/**
 * An {@link org.mule.runtime.dsl.api.component.ObjectFactory} which generates {@link TemplateOnErrorHandler} based on existing
 * ones.
 *
 * @since 4.1.0
 */
public class OnErrorFactoryBean extends AbstractComponentFactory<TemplateOnErrorHandler> {

  private final TemplateOnErrorHandler onErrorHandler;

  public OnErrorFactoryBean(TemplateOnErrorHandler onErrorHandler) {
    this.onErrorHandler = onErrorHandler;
  }

  @Override
  public TemplateOnErrorHandler doGetObject() throws Exception {
    return onErrorHandler;
  }

}
