/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
