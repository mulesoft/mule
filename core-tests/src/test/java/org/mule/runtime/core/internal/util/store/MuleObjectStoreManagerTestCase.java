/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.util.store;

import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.tck.SerializationTestUtils.addJavaSerializerToMockMuleContext;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.api.store.PartitionableObjectStore;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.store.PartitionedInMemoryObjectStore;
import org.mule.runtime.core.internal.store.PartitionedPersistentObjectStore;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.size.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.Serializable;
import java.util.NoSuchElementException;

@SmallTest
public class MuleObjectStoreManagerTestCase extends AbstractMuleTestCase {

  private static final String TEST_PARTITION_NAME = "partition";
  private static final int POLLING_TIMEOUT = 1000;
  private static final int POLLING_DELAY = 60;
  private static final String TEST_KEY = "Some Key";
  private static final String TEST_VALUE = "Some Value";

  private SimpleUnitTestSupportSchedulerService schedulerService;

  private MuleContextWithRegistries muleContext;
  private MuleObjectStoreManager storeManager;

  @Rule
  public TemporaryFolder tempWorkDir = new TemporaryFolder();

  @Before
  public void setup() {
    schedulerService = new SimpleUnitTestSupportSchedulerService();
    muleContext = mock(MuleContextWithRegistries.class);
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

  @After
  public void after() throws MuleException {
    schedulerService.stop();
  }

  @Test
  public void ensureTransientPartitionIsCleared() throws ObjectStoreException, InitialisationException {
    ensurePartitionIsCleared(false);
  }

  @Test
  public void ensurePersistentPartitionIsCleared() throws ObjectStoreException, InitialisationException {
    ensurePartitionIsCleared(true);
  }

  private void ensurePartitionIsCleared(boolean isPersistent) throws ObjectStoreException, InitialisationException {
    try {
      ObjectStorePartition<Serializable> store = createStorePartition(TEST_PARTITION_NAME, isPersistent);

      store.getBaseStore().store(TEST_KEY, TEST_VALUE, TEST_PARTITION_NAME);

      assertThat(store.allKeys().size(), is(1));

      storeManager.disposeStore(TEST_PARTITION_NAME);

      assertThat(store.allKeys().size(), is(0));
    } finally {
      storeManager.dispose();
    }
  }

  @Test
  public void removeStoreAndMonitorOnTransientPartition() throws ObjectStoreException, InitialisationException {
    removeStoreAndMonitor(false);
  }

  @Test
  public void removeStoreAndMonitorOnPersistentPartition() throws ObjectStoreException, InitialisationException {
    removeStoreAndMonitor(true);
  }

  private void removeStoreAndMonitor(boolean isPersistent) throws ObjectStoreException, InitialisationException {
    try {
      ObjectStorePartition<Serializable> store = createStorePartition(TEST_PARTITION_NAME, isPersistent);

      assertMonitorsCount(1);

      storeManager.disposeStore(TEST_PARTITION_NAME);

      try {
        storeManager.getObjectStore(TEST_PARTITION_NAME);
        fail("ObjectStore should not exist");
      } catch (NoSuchElementException e) {
        // shake it baby
      }

      assertMonitorsCount(0);
    } finally {
      storeManager.dispose();
    }
  }

  private void assertMonitorsCount(final int expectedValue) {
    new PollingProber(POLLING_TIMEOUT, POLLING_DELAY).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return assertMonitors(expectedValue);
      }

      private boolean assertMonitors(int expectedValue) {
        return storeManager.getMonitorsCount() == expectedValue;
      }

      @Override
      public String describeFailure() {
        return "Unexpected count of active monitors.";
      }
    });
  }

  private ObjectStorePartition<Serializable> createStorePartition(String partitionName, boolean isPersistent)
      throws InitialisationException {
    addJavaSerializerToMockMuleContext(muleContext);

    storeManager.initialise();

    ObjectStorePartition<Serializable> store =
        storeManager.createObjectStore(partitionName, ObjectStoreSettings.builder()
            .persistent(isPersistent)
            .entryTtl(10000L)
            .expirationInterval(50L)
            .build());

    assertThat(storeManager.getObjectStore(partitionName), is(sameInstance(store)));

    return store;
  }

  private void createRegistryAndBaseStore(MuleContextWithRegistries muleContext, Registry registry) {
    when(registry.lookupByName(BASE_PERSISTENT_OBJECT_STORE_KEY))
        .thenReturn(of(createPersistentPartitionableObjectStore(muleContext)));
    when(registry.lookupByName(BASE_IN_MEMORY_OBJECT_STORE_KEY)).thenReturn(of(createTransientPartitionableObjectStore()));
  }

  private PartitionableObjectStore<?> createTransientPartitionableObjectStore() {
    return new PartitionedInMemoryObjectStore<>();
  }

  private PartitionableObjectStore<?> createPersistentPartitionableObjectStore(MuleContext muleContext) {
    return new PartitionedPersistentObjectStore<>(muleContext);
  }
}
