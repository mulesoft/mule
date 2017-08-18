/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal.dsl.model.config;

import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import org.mule.runtime.api.component.location.ComponentProvider;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.config.RuntimeConfigurationException;

public class ConfigurationPropertiesException extends RuntimeConfigurationException implements ComponentProvider {

  private AnnotatedObject component;

  public ConfigurationPropertiesException(I18nMessage message, AnnotatedObject component) {
    super(message);
    checkNotNull(component, "component cannot be null");
    this.component = component;
  }

  public ConfigurationPropertiesException(I18nMessage message, AnnotatedObject component, Exception e) {
    super(message, e);
    checkNotNull(component, "component cannot be null");
    this.component = component;
  }

  @Override
  public AnnotatedObject getComponent() {
    return component;
  }
}
