/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.TransformationService;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;

public abstract class DomainFunctionalTestCase extends AbstractMuleTestCase {

  private final Map<String, MuleContext> muleContexts = new HashMap<>();
  private final List<MuleContext> disposedContexts = new ArrayList<>();
  private MuleContext domainContext;

  protected String getDomainConfig() {
    return null;
  }

  protected String[] getDomainConfigs() {
    return new String[] {getDomainConfig()};
  }

  public synchronized void disposeMuleContext(final MuleContext muleContext) {
    disposedContexts.add(muleContext);
    muleContext.dispose();
    new PollingProber(10000, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return muleContext.isDisposed();
      }

      @Override
      public String describeFailure() {
        return "mule context timeout during dispose";
      }
    });
  }

  @Before
  public void setUpMuleContexts() throws Exception {
    final DomainContextBuilder domainContextBuilder = new DomainContextBuilder() {

      @Override
      protected void addBuilders(List<ConfigurationBuilder> builders) {
        super.addBuilders(builders);
        if (getBuilder() != null) {
          builders.add(getBuilder());
        }
      }
    }.setDomainConfig(getDomainConfigs());

    domainContext = domainContextBuilder.build();
    ApplicationConfig[] applicationConfigs = getConfigResources();
    for (ApplicationConfig applicationConfig : applicationConfigs) {
      MuleContext muleContext = createAppMuleContext(applicationConfig.applicationResources);
      muleContexts.put(applicationConfig.applicationName, muleContext);
    }
  }

  @After
  public void disposeMuleContexts() {
    for (MuleContext muleContext : muleContexts.values()) {
      try {
        disposeMuleContext(muleContext);
      } catch (Exception e) {
        // Nothing to do
      }
    }
    muleContexts.clear();
    disposeMuleContext(domainContext);
  }

  protected MuleContext createAppMuleContext(String[] configResource) throws Exception {
    return new ApplicationContextBuilder().setDomainContext(domainContext).setApplicationResources(configResource).build();
  }

  public abstract ApplicationConfig[] getConfigResources();

  public MuleContext getMuleContextForApp(String applicationName) {
    return muleContexts.get(applicationName);
  }

  public MuleContext getMuleContextForDomain() {
    return domainContext;
  }

  protected ConfigurationBuilder getBuilder() {
    return null;
  }

  public static class ApplicationConfig {

    String applicationName;
    String[] applicationResources;

    public ApplicationConfig(String applicationName, String... applicationResources) {
      this.applicationName = applicationName;
      this.applicationResources = applicationResources;
    }
  }

  /**
   * Uses {@link TransformationService} to get a {@link String} representation of a message.
   *
   * @param message message to get payload from
   * @return String representation of the message payload
   * @throws Exception if there is an unexpected error obtaining the payload representation
   */
  protected String getPayloadAsString(Message message, MuleContext muleContext) throws Exception {
    return (String) muleContext.getTransformationService().transform(message, DataType.STRING).getPayload().getValue();
  }

}
