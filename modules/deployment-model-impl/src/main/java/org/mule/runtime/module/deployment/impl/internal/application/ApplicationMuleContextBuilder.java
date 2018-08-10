/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.registry.CompositeMuleRegistryHelper;
import org.mule.runtime.core.internal.registry.DefaultRegistryBroker;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;

import java.util.Map;

/**
 * Takes Mule application descriptor into account when building the context.
 */
public class ApplicationMuleContextBuilder extends SupportsPropertiesMuleContextBuilder {

  private final String appName;
  private final String defaultEncoding;
  private final MuleContext parentContext;

  public ApplicationMuleContextBuilder(String appName, Map<String, String> appProperties, String defaultEncoding,
                                       MuleContext parentContext) {
    super(APP, appProperties);
    this.appName = appName;
    this.defaultEncoding = defaultEncoding;
    this.parentContext = parentContext;
  }

  @Override
  protected DefaultMuleConfiguration createMuleConfiguration() {
    final DefaultMuleConfiguration configuration = new DefaultMuleConfiguration(true);
    initializeFromProperties(configuration);
    configuration.setId(appName);
    final String encoding = defaultEncoding;
    if (!isBlank(encoding)) {
      configuration.setDefaultEncoding(encoding);
    }
    return configuration;
  }

  @Override
  protected MuleRegistryHelper getMuleRegistry(DefaultMuleContext muleContext) {
    if (parentContext != null && ((DefaultMuleContext) parentContext).getRegistry() instanceof MuleRegistryHelper) {
      DefaultRegistryBroker registryBroker = new DefaultRegistryBroker(muleContext, muleContext.getLifecycleInterceptor());
      muleContext.setRegistryBroker(registryBroker);

      return new CompositeMuleRegistryHelper(registryBroker, muleContext, ((DefaultMuleContext) parentContext).getRegistry());
    } else {
      return super.getMuleRegistry(muleContext);
    }
  }
}
