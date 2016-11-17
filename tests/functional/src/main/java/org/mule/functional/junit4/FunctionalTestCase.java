/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;

import org.mule.functional.functional.FlowAssert;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.component.JavaComponent;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.component.AbstractJavaComponent;
import org.mule.runtime.core.construct.AbstractPipeline;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;

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

  public FunctionalTestCase() {
    super();
    // A functional test case that starts up the management context by default.
    setStartContext(true);
  }

  /**
   * @return
   * @deprecated use getConfigFile instead.
   */
  @Deprecated
  protected String getConfigResources() {
    return null;
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    String configResources = getConfigResources();
    if (configResources != null) {
      return new SpringXmlConfigurationBuilder(configResources, emptyMap(), APP);
    }
    configResources = getConfigFile();
    if (configResources != null) {
      if (configResources.contains(",")) {
        throw new RuntimeException("Do not use this method when the config is composed of several files. Use getConfigFiles method instead.");
      }
      return new SpringXmlConfigurationBuilder(configResources, emptyMap(), APP);
    }
    String[] multipleConfigResources = getConfigFiles();
    return new SpringXmlConfigurationBuilder(multipleConfigResources, emptyMap(), APP);
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
   * Returns an instance of the service's component object. Note that depending on the type of ObjectFactory used for the
   * component, this may create a new instance of the object. If you plan to set properties on the returned object, make sure your
   * component is declared as a singleton, otherwise this will not work.
   */
  protected Object getComponent(String serviceName) throws Exception {
    final FlowConstruct flowConstruct = muleContext.getRegistry().lookupObject(serviceName);

    if (flowConstruct != null) {
      return getComponent(flowConstruct);
    } else {
      throw new RegistrationException(I18nMessageFactory
          .createStaticMessage("Service " + serviceName + " not found in Registry"));
    }
  }

  /**
   * Returns an instance of the service's component object. Note that depending on the type of ObjectFactory used for the
   * component, this may create a new instance of the object. If you plan to set properties on the returned object, make sure your
   * component is declared as a singleton, otherwise this will not work.
   */
  protected Object getComponent(FlowConstruct flowConstruct) throws Exception {
    if (flowConstruct instanceof AbstractPipeline) {
      AbstractPipeline flow = (AbstractPipeline) flowConstruct;
      // Retrieve the first component
      for (Processor processor : flow.getMessageProcessors()) {
        if (processor instanceof Component) {
          return getComponentObject(((Component) processor));
        }
      }
    }

    throw new RegistrationException(I18nMessageFactory
        .createStaticMessage("Can't get component from flow construct " + flowConstruct.getName()));
  }

  /**
   * A convenience method to get a type-safe reference to the FunctionTestComponent
   * 
   * @param serviceName service name as declared in the config
   * @return test component
   * @since 2.2
   * @see FunctionalTestComponent
   */
  protected FunctionalTestComponent getFunctionalTestComponent(String serviceName) throws Exception {
    return (FunctionalTestComponent) getComponent(serviceName);
  }

  protected FlowConstruct getFlowConstruct(String flowName) throws Exception {
    return muleContext.getRegistry().lookupFlowConstruct(flowName);
  }

  @Override
  protected ClassLoader getExecutionClassLoader() {
    if (!isDisposeContextPerClass() || executionClassLoader == null) {
      executionClassLoader = new ContainerClassLoaderFactory().createContainerClassLoader(getClass().getClassLoader());
    }

    return executionClassLoader.getClassLoader();
  }

  @Override
  protected void doTearDown() throws Exception {
    executionClassLoader = null;
    super.doTearDown();
  }

  protected String loadResourceAsString(String resourceName) throws IOException {
    return IOUtils.getResourceAsString(resourceName, getClass());
  }

  protected InputStream loadResource(String resourceName) throws IOException {
    return IOUtils.getResourceAsStream(resourceName, getClass());
  }

  private Object getComponentObject(Component component) throws Exception {
    if (component instanceof JavaComponent) {
      return ((AbstractJavaComponent) component).getObjectFactory().getInstance(muleContext);
    } else {
      fail("Component is not a JavaComponent and therefore has no component object instance");
      return null;
    }
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
    return new FlowRunner(muleContext, flowName);
  }

  /**
   * Runs the given flow with a default event
   * 
   * @param flowName the name of the flow to be executed
   * @return the resulting <code>MuleEvent</code>
   * @throws Exception
   */
  protected Event runFlow(String flowName) throws Exception {
    return flowRunner(flowName).run();
  }

  /**
   * Retrieve a flow by name from the registry
   * 
   * @param name Name of the flow to retrieve
   */
  protected Flow lookupFlowConstruct(String name) {
    return (Flow) muleContext.getRegistry().lookupFlowConstruct(name);
  }

  @After
  public final void clearFlowAssertions() throws Exception {
    FlowAssert.reset();
  }

  protected MessageProcessorChain getSubFlow(String subflowName) {
    return (MessageProcessorChain) muleContext.getRegistry().lookupObject(subflowName);
  }
}
