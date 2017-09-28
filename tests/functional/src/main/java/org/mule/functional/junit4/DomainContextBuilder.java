/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.config.api.SpringXmlConfigurationBuilderFactory.createConfigurationBuilder;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.tck.config.TestServicesConfigurationBuilder;

import java.util.ArrayList;
import java.util.List;

public class DomainContextBuilder {

  private String[] domainConfig = new String[0];

  private MuleContextBuilder muleContextBuilder = MuleContextBuilder.builder(DOMAIN);

  public DomainContextBuilder setDomainConfig(String... domainConfig) {
    this.domainConfig = domainConfig;
    return this;
  }

  public MuleContext build() throws Exception {
    List<ConfigurationBuilder> builders = new ArrayList<>(3);
    ConfigurationBuilder cfgBuilder = getDomainBuilder(domainConfig);
    builders.add(cfgBuilder);
    addBuilders(builders);
    DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    MuleContext domainContext = muleContextFactory.createMuleContext(builders, muleContextBuilder);
    domainContext.start();
    return domainContext;
  }

  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(new TestServicesConfigurationBuilder());
  }

  protected ConfigurationBuilder getDomainBuilder(String[] configResources) throws Exception {
    return createConfigurationBuilder(configResources, emptyMap(), DOMAIN);
  }
}
