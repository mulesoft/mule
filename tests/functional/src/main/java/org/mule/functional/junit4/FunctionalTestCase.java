/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mule.runtime.container.api.ContainerClassLoaderProvider.createContainerClassLoader;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.retry.ReconnectionConfig.DISABLE_ASYNC_RETRY_POLICY_ON_SOURCES;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.createDefaultExtensionManager;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.ReconfigurableMuleConfiguration;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.processor.FlowAssert;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Rule;

/**
 * A base test case for tests that initialize Mule using a configuration file. The default configuration builder used is
 * SpringXmlConfigurationBuilder. To use this test case, ensure you have the mule-modules-builders JAR file on your classpath. To
 * use a different builder, just overload the <code>getBuilder()</code> method of this class to return the type of builder you
 * want to use with your test.
 */
public abstract class FunctionalTestCase extends AbstractMuleContextTestCase {

  /**
   * The executionClassLoader used to run this test. It will be created per class or per method depending on
   * {@link #disposeContextPerClass}.
   */
  private static ArtifactClassLoader executionClassLoader;

  @Inject
  protected Registry registry;

  private volatile boolean tearingDown = false;
  private final Set<FlowRunner> runners = new HashSet<>();

  public FunctionalTestCase() {
    super();
    // A functional test case that starts up the management context by default.
    setStartContext(true);
  }

  @Rule
  public SystemProperty muleDisableAsyncRetryPolicyOnSourcesProperty =
      new SystemProperty(DISABLE_ASYNC_RETRY_POLICY_ON_SOURCES, "true");

  /**
   * @return
   * @deprecated use getConfigFile instead.
   */
  @Deprecated
  protected String getConfigResources() {
    return null;
  }

  protected Map<String, String> artifactProperties() {
    return emptyMap();
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    ArtifactDeclaration artifactDeclaration = getArtifactDeclaration();
    if (artifactDeclaration != null) {
      return new ArtifactAstXmlParserConfigurationBuilder(artifactProperties(),
                                                          enableLazyInit(),
                                                          artifactDeclaration);
    }

    String configResources = getConfigResources();
    if (configResources != null) {
      return new ArtifactAstXmlParserConfigurationBuilder(artifactProperties(),
                                                          disableXmlValidations(), enableLazyInit(),
                                                          mustRegenerateExtensionModels(),
                                                          new String[] {configResources});
    }
    configResources = getConfigFile();
    if (configResources != null) {
      if (configResources.contains(",")) {
        throw new RuntimeException("Do not use this method when the config is composed of several files. Use getConfigFiles method instead.");
      }
      return new ArtifactAstXmlParserConfigurationBuilder(artifactProperties(),
                                                          disableXmlValidations(), enableLazyInit(),
                                                          mustRegenerateExtensionModels(),
                                                          new String[] {configResources});
    }
    return new ArtifactAstXmlParserConfigurationBuilder(artifactProperties(),
                                                        disableXmlValidations(), enableLazyInit(),
                                                        mustRegenerateExtensionModels(),
                                                        getConfigFiles());
  }

  public static ConfigurationBuilder extensionManagerWithMuleExtModelBuilder() {
    return extensionManagerWithMuleExtModelBuilder(singleton(getExtensionModel()));
  }

  public static ConfigurationBuilder extensionManagerWithMuleExtModelBuilder(Set<ExtensionModel> extensionModels) {
    return new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        ExtensionManager extensionManager = createExtensionManager(muleContext);
        extensionModels.forEach(extensionManager::registerExtension);
      }

    };
  }

  protected static ExtensionManager createExtensionManager(MuleContext muleContext) throws InitialisationException {
    ExtensionManager extensionManager;
    if (muleContext.getExtensionManager() == null) {
      extensionManager = createDefaultExtensionManager();
      muleContext.setExtensionManager(extensionManager);
      initialiseIfNeeded(extensionManager, muleContext);
    }
    extensionManager = muleContext.getExtensionManager();
    return extensionManager;
  }

  /**
   * @return a single file that defines a mule application configuration
   */
  protected String getConfigFile() {
    return null;
  }

  /**
   * @return a several files that define a mule application configuration
   */
  protected String[] getConfigFiles() {
    return null;
  }

  /**
   * @return {@link ArtifactDeclaration} that defines the mule application configuration.
   */
  protected ArtifactDeclaration getArtifactDeclaration() {
    return null;
  }

  protected FlowConstruct getFlowConstruct(String flowName) throws Exception {
    return registry.<FlowConstruct>lookupByName(flowName).get();
  }

  @Override
  protected ClassLoader getExecutionClassLoader() {
    if (!isDisposeContextPerClass() || executionClassLoader == null) {
      executionClassLoader = createContainerClassLoader(new FunctionalTestModuleRepository(), getClass().getClassLoader());
    }

    return executionClassLoader.getClassLoader();
  }

  @Override
  protected void doTearDown() throws Exception {
    synchronized (runners) {
      tearingDown = true;
      for (FlowRunner runner : runners) {
        runner.dispose();
      }
    }
    super.doTearDown();
  }

  protected String loadResourceAsString(String resourceName) throws IOException {
    return IOUtils.getResourceAsString(resourceName, getClass());
  }

  protected InputStream loadResource(String resourceName) throws IOException {
    return IOUtils.getResourceAsStream(resourceName, getClass());
  }

  protected void stopFlowConstruct(String flowName) throws Exception {
    FlowConstruct flowConstruct = getFlowConstruct(flowName);
    Flow flow = (Flow) flowConstruct;
    flow.stop();
  }

  /**
   * Initializes a builder to construct an event and the running context to run it through a flow.
   *
   * @param flowName
   * @return the {@link FlowRunner}
   */
  protected FlowRunner flowRunner(String flowName) {
    synchronized (runners) {
      if (tearingDown) {
        throw new IllegalStateException("Already tearing down.");
      }
      final FlowRunner flowRunner = new FlowRunner(registry, flowName);
      runners.add(flowRunner);
      return flowRunner;
    }
  }

  /**
   * Runs the given flow with a default event
   *
   * @param flowName the name of the flow to be executed
   * @return the resulting <code>MuleEvent</code>
   * @throws Exception
   */
  protected CoreEvent runFlow(String flowName) throws Exception {
    return flowRunner(flowName).run();
  }

  @After
  public final void clearFlowAssertions() throws Exception {
    FlowAssert.reset();
  }

  /**
   * @return a boolean indicating if the Mule App should start in lazy mode. This means that the Mule App components will be
   *         initialized on demand.
   */
  public boolean enableLazyInit() {
    return false;
  }

  @Override
  protected DefaultMuleConfiguration createMuleConfiguration() {
    if (enableLazyInit()) {
      return new ReconfigurableMuleConfiguration();
    } else {
      return super.createMuleConfiguration();
    }
  }

  /**
   * @return a boolean indicating if the Mule App should start without XML Validations.
   */
  public boolean disableXmlValidations() {
    return false;
  }

  /**
   * Subclasses can override this method so that extension models used are regenerated before running its tests. For example, some
   * part of a extension model might only be created if a certain system property is in place, so the test classes that test that
   * feature will have to generate the extension model when the property is already set.
   *
   * @return whether the tests on this class need for extensions model to be generated again.
   */
  protected boolean mustRegenerateExtensionModels() {
    return false;
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(extensionManagerWithMuleExtModelBuilder(getExtensionModels()));
    super.addBuilders(builders);
  }

  protected Set<ExtensionModel> getExtensionModels() {
    return singleton(getExtensionModel());
  }
}
