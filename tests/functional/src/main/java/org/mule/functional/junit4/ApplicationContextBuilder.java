/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mule.runtime.config.api.SpringXmlConfigurationBuilderFactory.createConfigurationBuilder;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.MuleContextFactory;

import java.util.ArrayList;
import java.util.List;

public class ApplicationContextBuilder {

  private MuleContext domainContext;
  private String[] applicationResources = new String[0];

  private MuleContextBuilder muleContextBuilder = MuleContextBuilder.builder(APP);

  public ApplicationContextBuilder setDomainContext(MuleContext domainContext) {
    this.domainContext = domainContext;
    return this;
  }

  public ApplicationContextBuilder setApplicationResources(String[] applicationResources) {
    this.applicationResources = applicationResources;
    return this;
  }

  public MuleContext build() throws Exception {
    // Should we set up the manager for every method?
    MuleContext context = doBuildContext();
    context.start();
    return context;
  }

  protected MuleContext doBuildContext() throws Exception {
    MuleContext context;
    MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    List<ConfigurationBuilder> builders = new ArrayList<>();
    builders.add(getAppBuilder(this.applicationResources));
    addBuilders(builders);
    configureMuleContext(muleContextBuilder);
    context = muleContextFactory.createMuleContext(builders, muleContextBuilder);
    return context;
  }

  // This shouldn't be needed by Test cases but can be used by base testcases that wish to add further builders when
  // creating the MuleContext.
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    // No op
  }

  protected ConfigurationBuilder getAppBuilder(String[] configResource) throws Exception {
    return createConfigurationBuilder(configResource, domainContext);
  }

  /**
   * Override this method to set properties of the MuleContextBuilder before it is used to create the MuleContext.
   */
  protected void configureMuleContext(MuleContextBuilder contextBuilder) {}
}
