/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.getExtensionModel;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import jakarta.transaction.TransactionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

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

    @Inject
    private TransformationService transformationService;

    @Inject
    private ArtifactEncoding artifactEncoding;

    @Inject
    private Optional<TransactionManager> transactionManager;

    public Registry getRegistry() {
      return registry;
    }

    public MuleContext getMuleContext() {
      return muleContext;
    }

    public SchedulerService getSchedulerService() {
      return schedulerService;
    }

    public NotificationListenerRegistry getNotificationListenerRegistry() {
      return notificationListenerRegistry;
    }

    public TransformationService getTransformationService() {
      return transformationService;
    }

    public ArtifactEncoding getArtifactEncoding() {
      return artifactEncoding;
    }

    public TransactionManager getTransactionManager() {
      return transactionManager.orElse(null);
    }

  }

  /**
   * System property to set the enforcement policy. Defined here as a decision was made not to expose it as an API yet. For now,
   * it will be for internal use only.
   *
   * @since 4.5.0
   */
  static final String EXTENSION_JVM_ENFORCEMENT_PROPERTY = SYSTEM_PROPERTY_PREFIX + "jvm.version.extension.enforcement";
  static final String JVM_ENFORCEMENT_LOOSE = "LOOSE";

  // This is needed apart from the setting in {@code @ArtifactClassLoaderRunnerConfig} because tha validation also takes place
  // during extension registering, not only during its discovery.
  @Rule
  public SystemProperty jvmVersionExtensionEnforcementLoose =
      new SystemProperty(EXTENSION_JVM_ENFORCEMENT_PROPERTY, JVM_ENFORCEMENT_LOOSE);

  private final Map<String, MuleContext> muleContexts = new HashMap<>();
  private final Map<String, ArtifactInstanceInfrastructure> applsInfrastructures = new HashMap<>();
  private final List<MuleContext> disposedContexts = new ArrayList<>();
  private MuleContext domainContext;
  private ArtifactContext domainArtifactContext;
  private ArtifactInstanceInfrastructure domainInfrastructure;

  protected String getDomainConfig() {
    return null;
  }

  protected String[] getDomainConfigs() {
    return new String[] {getDomainConfig()};
  }

  /**
   * @return whether the applications and domains should start in lazy mode. This means that the components will be initialized on
   *         demand.
   * @since 4.5
   * @see FunctionalTestCase#enableLazyInit()
   */
  protected boolean enableLazyInit() {
    return false;
  }

  /**
   * @return whether the applications and domains should be parsed without XML Validations.
   * @since 4.5
   * @see FunctionalTestCase#disableXmlValidations()
   */
  protected boolean disableXmlValidations() {
    return false;
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
        .setEnableLazyInit(enableLazyInit())
        .setDisableXmlValidations(disableXmlValidations())
        .setDomainConfig(getDomainConfigs());

    domainArtifactContext = domainContextBuilder.build();
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

  protected Map<String, Object> getAppStartUpRegistryObjects() {
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

  protected ArtifactInstanceInfrastructure createAppMuleContext(ApplicationConfig applicationConfig) throws Exception {
    MuleContext muleContext = createAppMuleContext(applicationConfig.applicationResources, domainArtifactContext);
    ArtifactInstanceInfrastructure appInfrasturcture = new ArtifactInstanceInfrastructure();
    muleContext.getInjector().inject(appInfrasturcture);
    return appInfrasturcture;
  }

  private MuleContext createAppMuleContext(String[] configResource, ArtifactContext domainArtifactContext) throws Exception {
    return new ApplicationContextBuilder() {

      @Override
      protected void addBuilders(List<ConfigurationBuilder> builders) {
        super.addBuilders(builders);
        builders.add(new SimpleConfigurationBuilder(getAppStartUpRegistryObjects()));
      }

      @Override
      protected Set<ExtensionModel> getExtensionModels() {
        return DomainFunctionalTestCase.this.getExtensionModels();
      }
    }
        .setContextId(this.getClass().getSimpleName())
        .setDomainArtifactContext(domainArtifactContext)
        .setApplicationResources(configResource)
        .setArtifactCoordinates(getTestArtifactCoordinates())
        .setEnableLazyInit(enableLazyInit())
        .setDisableXmlValidations(disableXmlValidations())
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
