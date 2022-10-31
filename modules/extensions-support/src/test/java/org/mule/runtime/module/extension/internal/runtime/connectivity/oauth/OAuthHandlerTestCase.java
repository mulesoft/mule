/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static org.mule.runtime.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CLASSLOADER_REPOSITORY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.context.DefaultMuleContext.currentMuleContext;

import static java.lang.Thread.currentThread;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.PartitionableObjectStore;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.internal.config.builders.ServiceCustomizationsConfigurationBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.store.PartitionedInMemoryObjectStore;
import org.mule.runtime.core.internal.store.PartitionedPersistentObjectStore;
import org.mule.runtime.core.internal.util.store.MuleObjectStoreManager;
import org.mule.runtime.core.internal.util.store.MuleObjectStoreManagerTestCase;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeOAuthHandler;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.MockExtensionManagerConfigurationBuilder;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OAuthHandlerTestCase extends AbstractMuleContextTestCase {

  // private MuleContext muleContext;
  AuthorizationCodeOAuthHandler oauthHandler;
  private MuleContextWithRegistry muleContext;
  private MuleObjectStoreManager storeManager;

  private SimpleUnitTestSupportSchedulerService schedulerService;
  private volatile CountDownLatch expireDelayLatch = new CountDownLatch(0);
  private AtomicInteger expires = new AtomicInteger();

  @Test
  public void initialise() throws InitialisationException {
    oauthHandler = new AuthorizationCodeOAuthHandler();
    oauthHandler.initialise();
  }

  @Rule
  public TemporaryFolder tempWorkDir = new TemporaryFolder();

  @Before
  public void setup() {
    schedulerService = new SimpleUnitTestSupportSchedulerService();
    muleContext = mock(MuleContextWithRegistry.class);
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    when(muleConfiguration.getWorkingDirectory()).thenReturn(tempWorkDir.getRoot().getAbsolutePath());
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);

    Registry registry = mock(Registry.class);
    createRegistryAndBaseStore(muleContext, registry);
    when(muleContext.getSchedulerBaseConfig())
        .thenReturn(config().withPrefix(MuleObjectStoreManagerTestCase.class.getName() + "#" + name.getMethodName()));

    storeManager = new MuleObjectStoreManager();
    storeManager.setSchedulerService(schedulerService);
    storeManager.setRegistry(registry);
    storeManager.setMuleContext(muleContext);
  }

  private void createRegistryAndBaseStore(MuleContextWithRegistry muleContext, Registry registry) {
    when(registry.lookupByName(BASE_PERSISTENT_OBJECT_STORE_KEY))
        .thenReturn(of(createPersistentPartitionableObjectStore(muleContext)));
    when(registry.lookupByName(BASE_IN_MEMORY_OBJECT_STORE_KEY)).thenReturn(of(createTransientPartitionableObjectStore()));
  }

  private PartitionableObjectStore<?> createPersistentPartitionableObjectStore(MuleContext muleContext) {
    return new PartitionedPersistentObjectStore(muleContext) {

      @Override
      public void expire(long entryTTL, int maxEntries, String partitionName) throws ObjectStoreException {
        expires.incrementAndGet();
        super.expire(entryTTL, maxEntries, partitionName);
        expireDelay();
      }
    };
  }

  private PartitionableObjectStore<?> createTransientPartitionableObjectStore() {
    return new PartitionedInMemoryObjectStore() {

      @Override
      public void expire(long entryTTL, int maxEntries, String partitionName) throws ObjectStoreException {
        expires.incrementAndGet();
        super.expire(entryTTL, maxEntries, partitionName);
        expireDelay();
      }
    };
  }

  private void expireDelay() {
    try {
      expireDelayLatch.await();
    } catch (InterruptedException e) {
      currentThread().interrupt();
      return;
    }
  }

  /*
   * @After public void tearDown() { currentMuleContext.set(null); }
   */
  @Override
  protected void doTearDown() throws Exception {

    // template method
    oauthHandler.stop();
    currentMuleContext.set(null);
  }


  @After
  public void after() throws MuleException {
    schedulerService.stop();
  }
  /*
   * @Before public void doSetUp() throws Exception { super.doSetUp(); muleContext = createMuleContext();
   * currentMuleContext.set(muleContext); // initialiseIfNeeded(serializationProtocol, true, muleContext); }
   */

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(OBJECT_CLASSLOADER_REPOSITORY, new ClassLoaderRepository() {

      @Override
      public Optional<String> getId(ClassLoader classLoader) {
        return null;
      }

      @Override
      public Optional<ClassLoader> find(String classLoaderId) {
        return null;
      }
    });
  }
}
