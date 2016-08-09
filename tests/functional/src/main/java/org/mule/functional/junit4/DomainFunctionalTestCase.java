/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleMessage;
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

  private final Map<String, MuleContext> muleContexts = new HashMap<String, MuleContext>();
  private final List<MuleContext> disposedContexts = new ArrayList<MuleContext>();
  private MuleContext domainContext;

  protected abstract String getDomainConfig();

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
    domainContext = new DomainContextBuilder().setDomainConfig(getDomainConfig()).build();
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

  protected ConfigurationBuilder getBuilder() throws Exception {
    return null;
  }

  public class ApplicationConfig {

    String applicationName;
    String[] applicationResources;

    public ApplicationConfig(String applicationName, String[] applicationResources) {
      this.applicationName = applicationName;
      this.applicationResources = applicationResources;
    }
  }

  /**
   * Uses {@link org.mule.runtime.core.TransformationService} to get a {@link String} representation of a message.
   *
   * @param message message to get payload from
   * @return String representation of the message payload
   * @throws Exception if there is an unexpected error obtaining the payload representation
   */
  protected String getPayloadAsString(MuleMessage message, MuleContext muleContext) throws Exception {
    return (String) muleContext.getTransformationService().transform(message, DataType.STRING).getPayload();
  }

}
