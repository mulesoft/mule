/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getExtensionModel;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;

public abstract class DomainFunctionalTestCase extends AbstractMuleTestCase {

  /**
   * Provides access for infrastructure objects of each deployed Mule artifact.
   */
  protected static class ArtifactInstanceInfrastructure {

    @Inject
    private Registry registry;

    @Inject
    private MuleContext muleContext;

    @Inject
    private SchedulerService schedulerService;

    @Inject
    private NotificationListenerRegistry notificationListenerRegistry;

    public Registry getRegistry() {
      return registry;
    }

    public MuleContext getMuleContext() {
      return muleContext;
    }

    public SchedulerService getSchedulerService() {
      return getMuleContext().getSchedulerService();
    }

    public NotificationListenerRegistry getNotificationListenerRegistry() {
      return notificationListenerRegistry;
    }

  }

  private final Map<String, MuleContext> muleContexts = new HashMap<>();
  private final Map<String, ArtifactInstanceInfrastructure> applsInfrastructures = new HashMap<>();
  private final List<MuleContext> disposedContexts = new ArrayList<>();
  private MuleContext domainContext;
  private ArtifactInstanceInfrastructure domainInfrastructure;

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
        builders.add(new SimpleConfigurationBuilder(getDomainStartUpRegistryObjects()));
        if (getBuilder() != null) {
          builders.add(getBuilder());
        }
      }

      @Override
      protected Set<ExtensionModel> getExtensionModels() {
        return DomainFunctionalTestCase.this.getExtensionModels();
      }
    }
        .setContextId(this.getClass().getSimpleName())
        .setDomainConfig(getDomainConfigs());

    final ArtifactContext domainArtifactContext = domainContextBuilder.build();
    domainContext = domainArtifactContext.getMuleContext();
    domainInfrastructure = new ArtifactInstanceInfrastructure();
    domainContext.getInjector().inject(domainInfrastructure);
    ApplicationConfig[] applicationConfigs = getConfigResources();

    for (ApplicationConfig applicationConfig : applicationConfigs) {
      MuleContext muleContext = createAppMuleContext(applicationConfig.applicationResources, domainArtifactContext);
      muleContexts.put(applicationConfig.applicationName, muleContext);

      ArtifactInstanceInfrastructure appInfrasturcture = new ArtifactInstanceInfrastructure();
      muleContext.getInjector().inject(appInfrasturcture);
      applsInfrastructures.put(applicationConfig.applicationName, appInfrasturcture);
    }
  }

  protected Set<ExtensionModel> getExtensionModels() {
    return singleton(getExtensionModel());
  }

  protected Map<String, Object> getDomainStartUpRegistryObjects() {
    return emptyMap();
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
    applsInfrastructures.clear();
    disposeMuleContext(domainContext);
    domainInfrastructure = null;
  }

  private MuleContext createAppMuleContext(String[] configResource, ArtifactContext domainArtifactContext) throws Exception {
    return new ApplicationContextBuilder() {

      @Override
      protected Set<ExtensionModel> getExtensionModels() {
        return DomainFunctionalTestCase.this.getExtensionModels();
      }
    }
        .setContextId(this.getClass().getSimpleName())
        .setDomainArtifactContext(domainArtifactContext)
        .setApplicationResources(configResource)
        .build();
  }

  public abstract ApplicationConfig[] getConfigResources();

  public MuleContext getMuleContextForApp(String applicationName) {
    return muleContexts.get(applicationName);
  }

  public ArtifactInstanceInfrastructure getInfrastructureForApp(String applicationName) {
    return applsInfrastructures.get(applicationName);
  }

  public MuleContext getMuleContextForDomain() {
    return domainContext;
  }

  public ArtifactInstanceInfrastructure getDomainInfrastructure() {
    return domainInfrastructure;
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
   * Uses {@link org.mule.runtime.api.transformation.TransformationService} to get a {@link String} representation of a message.
   *
   * @param message message to get payload from
   * @return String representation of the message payload
   * @throws Exception if there is an unexpected error obtaining the payload representation
   */
  protected String getPayloadAsString(Message message, MuleContext muleContext) throws Exception {
    return (String) muleContext.getTransformationService().transform(message, DataType.STRING).getPayload().getValue();
  }

}
