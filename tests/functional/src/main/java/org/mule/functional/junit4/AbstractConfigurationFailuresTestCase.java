/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STARTED;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;

import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.CraftedExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.tck.config.TestNotificationListenerRegistryConfigurationBuilder;
import org.mule.tck.config.TestPolicyProviderConfigurationBuilder;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;

public abstract class AbstractConfigurationFailuresTestCase extends AbstractMuleTestCase {

  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder =
      new TestServicesConfigurationBuilder(true, mockExpressionExecutor());

  protected void loadConfiguration(String configuration) throws MuleException, InterruptedException {

    MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    List<ConfigurationBuilder> builders = new ArrayList<>();
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        DefaultExtensionManager defaultExtensionManager = new DefaultExtensionManager();
        initialiseIfNeeded(defaultExtensionManager, muleContext);
        getRequiredExtensions().forEach(defaultExtensionManager::registerExtension);
        muleContext.setExtensionManager(defaultExtensionManager);
      }
    });
    ConfigurationBuilder configurationBuilder = new ArtifactAstXmlParserConfigurationBuilder(emptyMap(),
                                                                                             enableLazyInit(),
                                                                                             disableXmlValidations(),
                                                                                             false,
                                                                                             new String[] {configuration});
    configurationBuilder.addServiceConfigurator(testServicesConfigurationBuilder);
    builders.add(configurationBuilder);
    builders.add(testServicesConfigurationBuilder);
    builders.add(new TestPolicyProviderConfigurationBuilder());
    builders.add(new TestNotificationListenerRegistryConfigurationBuilder());
    MuleContextBuilder contextBuilder = MuleContextBuilder.builder(APP);
    final DefaultMuleConfiguration muleConfiguration = new DefaultMuleConfiguration();
    muleConfiguration.setId(AbstractConfigurationFailuresTestCase.class.getSimpleName());
    applyConfiguration(muleConfiguration);
    contextBuilder.setMuleConfiguration(muleConfiguration);
    // Simulate the classloader for the app
    contextBuilder.setExecutionClassLoader(new ClassLoader(currentThread().getContextClassLoader()) {});
    MuleContextWithRegistry muleContext =
        (MuleContextWithRegistry) muleContextFactory.createMuleContext(builders, contextBuilder);
    final AtomicReference<Latch> contextStartedLatch = new AtomicReference<>();
    contextStartedLatch.set(new Latch());
    NotificationListenerRegistry notificationListenerRegistry =
        muleContext.getRegistry().get(NotificationListenerRegistry.REGISTRY_KEY);
    notificationListenerRegistry.registerListener(new MuleContextNotificationListener<MuleContextNotification>() {

      @Override
      public boolean isBlocking() {
        return false;
      }

      @Override
      public void onNotification(MuleContextNotification notification) {
        if (new IntegerAction(CONTEXT_STARTED).equals(notification.getAction())) {
          contextStartedLatch.get().countDown();
        }
      }
    });
    muleContext.start();
    try {
      assertThat(contextStartedLatch.get().await(20, SECONDS), is(true));
    } finally {
      muleContext.stop();
      muleContext.dispose();
    }
  }

  protected void applyConfiguration(DefaultMuleConfiguration muleConfiguration) {
    // nothing to do by default
  }

  protected boolean disableXmlValidations() {
    return false;
  }

  protected boolean enableLazyInit() {
    return false;
  }

  protected boolean mockExpressionExecutor() {
    return true;
  }

  protected List<ExtensionModel> getRequiredExtensions() {
    return singletonList(getExtensionModel());
  }

  protected ExtensionModel loadExtension(Class extension, Set<ExtensionModel> deps) {
    DefaultJavaExtensionModelLoader loader = new DefaultJavaExtensionModelLoader();
    return loadExtensionWithLoader(extension, deps, loader);
  }

  protected ExtensionModel loadExtensionWithDelegate(Class extension, Set<ExtensionModel> deps) {
    CraftedExtensionModelLoader loader = new CraftedExtensionModelLoader();
    return loadExtensionWithLoader(extension, deps, loader);
  }

  protected ExtensionModel loadExtensionWithLoader(Class extension, Set<ExtensionModel> deps,
                                                   ExtensionModelLoader extensionModelLoader) {
    Map<String, Object> ctx = new HashMap<>();
    ctx.put(TYPE_PROPERTY_NAME, extension.getName());
    ctx.put(VERSION, getProductVersion());
    ctx.putAll(getExtensionLoaderContextAdditionalParameters());
    return extensionModelLoader.loadExtensionModel(currentThread().getContextClassLoader(), DslResolvingContext.getDefault(deps),
                                                   ctx);
  }

  /**
   * Subclasses can override this method so that extension models are generated with an extension loading context that contains
   * the parameters returned by this method.
   *
   * @return a map with parameters to be added to the extension loader context.
   */
  protected Map<String, Object> getExtensionLoaderContextAdditionalParameters() {
    return emptyMap();
  }

}
