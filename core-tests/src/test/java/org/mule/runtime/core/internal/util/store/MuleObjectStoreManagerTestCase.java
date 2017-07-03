/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.util.store;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.api.store.ObjectStoreManager.UNBOUNDED;
import static org.mule.tck.SerializationTestUtils.addJavaSerializerToMockMuleContext;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.store.PartitionableObjectStore;
import org.mule.runtime.core.api.store.PartitionedInMemoryObjectStore;
import org.mule.runtime.core.api.store.PartitionedPersistentObjectStore;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class MuleObjectStoreManagerTestCase extends AbstractMuleTestCase {

  private static final String TEST_PARTITION_NAME = "partition";
  private static final int POLLING_TIMEOUT = 1000;
  private static final int POLLING_DELAY = 60;
  private static final String TEST_KEY = "Some Key";
  private static final String TEST_VALUE = "Some Value";

  private SimpleUnitTestSupportSchedulerService schedulerService;

  private MuleContext muleContext;
  private MuleObjectStoreManager storeManager;

  @Rule
  public TemporaryFolder tempWorkDir = new TemporaryFolder();

  @Before
  public void setup() {
    schedulerService = new SimpleUnitTestSupportSchedulerService();
    muleContext = mock(MuleContext.class);
    when(muleContext.getSchedulerService()).thenReturn(schedulerService);
    when(muleContext.getSchedulerBaseConfig())
        .thenReturn(config().withPrefix(MuleObjectStoreManagerTestCase.class.getName() + "#" + name.getMethodName()));

    storeManager = new MuleObjectStoreManager();
    storeManager.setMuleContext(muleContext);
  }

  @After
  public void after() throws MuleException {
    schedulerService.stop();
  }

  @Test
  public void disposeDisposableStore() throws ObjectStoreException {
    @SuppressWarnings("unchecked")
    ObjectStore<Serializable> store = mock(ObjectStore.class, withSettings().extraInterfaces(Disposable.class));

    this.storeManager.disposeStore(store);

    verify(store).clear();
    verify((Disposable) store).dispose();
  }

  @Test
  public void disposePartitionableStore() throws ObjectStoreException {
    @SuppressWarnings("unchecked")
    ObjectStorePartition<Serializable> store =
        mock(ObjectStorePartition.class,
             withSettings().extraInterfaces(Disposable.class).defaultAnswer(RETURNS_DEEP_STUBS));

    when(store.getPartitionName()).thenReturn(TEST_PARTITION_NAME);

    storeManager.disposeStore(store);

    verify(store.getBaseStore()).disposePartition(TEST_PARTITION_NAME);
    verify(store, never()).clear();
    verify((Disposable) store).dispose();
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

      storeManager.disposeStore(store);

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

      storeManager.disposeStore(store);

      assertThat(storeManager.stores.keySet(), not(hasItem(TEST_PARTITION_NAME)));

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

    createRegistryAndBaseStore(muleContext, isPersistent);

    storeManager.setMuleContext(muleContext);
    storeManager.initialise();

    ObjectStorePartition<Serializable> store =
        storeManager.getObjectStore(partitionName, isPersistent, UNBOUNDED, 10000, 50);

    assertThat(storeManager.stores.keySet(), hasItem(partitionName));

    return store;
  }

  private void createRegistryAndBaseStore(MuleContext muleContext, boolean isPersistent) {
    MuleRegistry muleRegistry = mock(MuleRegistry.class);
    if (isPersistent) {
      PartitionableObjectStore<?> store = createPersistentPartitionableObjectStore(muleContext);
      when(muleRegistry.lookupObject(OBJECT_STORE_DEFAULT_PERSISTENT_NAME)).thenReturn(store);
    } else {
      PartitionableObjectStore<?> store = createTransientPartitionableObjectStore();
      when(muleRegistry.lookupObject(OBJECT_STORE_DEFAULT_IN_MEMORY_NAME)).thenReturn(store);
    }

    when(muleContext.getRegistry()).thenReturn(muleRegistry);
  }

  private PartitionableObjectStore<?> createTransientPartitionableObjectStore() {
    return new PartitionedInMemoryObjectStore<>();
  }

  private PartitionableObjectStore<?> createPersistentPartitionableObjectStore(MuleContext muleContext) {
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    when(muleConfiguration.getWorkingDirectory()).thenReturn(tempWorkDir.getRoot().getAbsolutePath());
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);

    return new PartitionedPersistentObjectStore<>(muleContext);
  }

  @Test
  public void dontFailIfUnsupported() throws ObjectStoreException {
    @SuppressWarnings("unchecked")
    ObjectStore<Serializable> store = mock(ObjectStore.class, withSettings().extraInterfaces(Disposable.class));

    doThrow(UnsupportedOperationException.class).when(store).clear();

    storeManager.disposeStore(store);

    verify(store).clear();
    verify((Disposable) store).dispose();
  }

}
