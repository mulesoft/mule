/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.store;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import static org.mule.tck.probe.PollingProber.check;

import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.internal.util.store.MonitoredObjectStoreWrapper.StoredObject;
import org.mule.tck.core.util.store.InMemoryObjectStore;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import io.qameta.allure.Issue;

@SmallTest
public class MonitoredObjectStoreWrapperTestCase extends AbstractMuleTestCase {

  private static final String KEY = "key";
  private static final String OTHER_KEY = "otherKey";

  private static final int GC_POLLING_TIMEOUT = 10000;

  @Rule
  public MockitoRule mockitoRule = rule().silent();

  @Mock
  private ObjectStoreSettings settings;

  @Mock
  private ObjectStore<StoredObject> objectStore;
  private MonitoredObjectStoreWrapper wrapper;

  @Test
  public void expireWithoutMaxEntries() throws Exception {
    when(settings.getMaxEntries()).thenReturn(empty());
    when(settings.getEntryTTL()).thenReturn(of(1L));

    StoredObject<String> value = new StoredObject<>("", 0L, KEY);
    when(objectStore.allKeys()).thenReturn(asList(KEY));
    when(objectStore.retrieve(KEY)).thenReturn(value);
    when(objectStore.contains(KEY)).thenReturn(true);
    when(objectStore.remove(KEY)).thenReturn(value);

    wrapper = new MonitoredObjectStoreWrapper(objectStore, settings);

    wrapper.expire();

    check(5000, 100, () -> {
      verify(objectStore).remove(KEY);
      return true;
    });
  }

  @Test
  public void expireWithMaxEntries() throws Exception {
    when(settings.getMaxEntries()).thenReturn(of(1));
    when(settings.getEntryTTL()).thenReturn(empty());

    StoredObject<String> value1 = new StoredObject<>("", 0L, KEY);
    StoredObject<String> value2 = new StoredObject<>("", 1L, OTHER_KEY);
    when(objectStore.allKeys()).thenReturn(asList(KEY, OTHER_KEY));
    when(objectStore.retrieve(KEY)).thenReturn(value1);
    when(objectStore.contains(KEY)).thenReturn(true);
    when(objectStore.remove(KEY)).thenReturn(value1);

    when(objectStore.retrieve(OTHER_KEY)).thenReturn(value2);
    when(objectStore.contains(OTHER_KEY)).thenReturn(true);
    when(objectStore.remove(OTHER_KEY)).thenReturn(value2);

    wrapper = new MonitoredObjectStoreWrapper(objectStore, settings);

    wrapper.expire();

    check(5000, 100, () -> {
      verify(objectStore).remove(KEY);
      verify(objectStore, never()).remove(OTHER_KEY);
      return true;
    });
  }

  @Test
  public void expireWhileEntryRemovedAfterAllKeys() throws Exception {
    when(settings.getMaxEntries()).thenReturn(empty());
    when(settings.getEntryTTL()).thenReturn(of(1L));

    StoredObject<String> value = new StoredObject<>("", 0L, KEY);
    when(objectStore.allKeys()).thenReturn(asList(KEY, OTHER_KEY));
    when(objectStore.retrieve(KEY)).thenThrow(ObjectDoesNotExistException.class);
    when(objectStore.contains(KEY)).thenReturn(false);
    when(objectStore.remove(KEY)).thenThrow(ObjectDoesNotExistException.class);

    when(objectStore.retrieve(OTHER_KEY)).thenReturn(value);
    when(objectStore.contains(OTHER_KEY)).thenReturn(true);
    when(objectStore.remove(OTHER_KEY)).thenReturn(value);

    wrapper = new MonitoredObjectStoreWrapper(objectStore, settings);

    wrapper.expire();

    check(5000, 100, () -> {
      verify(objectStore).remove(OTHER_KEY);
      return true;
    });
  }

  @Test
  public void expireWithMaxEntriesWhileEntryRemovedAfterAllKeys() throws Exception {
    when(settings.getMaxEntries()).thenReturn(of(1));
    when(settings.getEntryTTL()).thenReturn(empty());

    StoredObject<String> value2 = new StoredObject<>("", 1L, OTHER_KEY);
    when(objectStore.allKeys()).thenReturn(asList(KEY, OTHER_KEY));
    when(objectStore.retrieve(KEY)).thenThrow(ObjectDoesNotExistException.class);
    when(objectStore.contains(KEY)).thenReturn(false);
    when(objectStore.remove(KEY)).thenThrow(ObjectDoesNotExistException.class);

    when(objectStore.retrieve(OTHER_KEY)).thenReturn(value2);
    when(objectStore.contains(OTHER_KEY)).thenReturn(true);
    when(objectStore.remove(OTHER_KEY)).thenReturn(value2);

    wrapper = new MonitoredObjectStoreWrapper(objectStore, settings);

    wrapper.expire();

    check(5000, 100, () -> {
      verify(objectStore, never()).remove(OTHER_KEY);
      return true;
    });
  }

  @Test
  public void expireWhileEntryRemovedAfterRetrieve() throws Exception {
    when(settings.getMaxEntries()).thenReturn(empty());
    when(settings.getEntryTTL()).thenReturn(of(1L));

    StoredObject<String> value = new StoredObject<>("", currentTimeMillis(), KEY);
    when(objectStore.allKeys()).thenReturn(asList(KEY, OTHER_KEY));
    when(objectStore.retrieve(KEY)).thenReturn(value);
    when(objectStore.contains(KEY)).thenReturn(false);
    when(objectStore.remove(KEY)).thenThrow(ObjectDoesNotExistException.class);

    when(objectStore.retrieve(OTHER_KEY)).thenReturn(value);
    when(objectStore.contains(OTHER_KEY)).thenReturn(true);
    when(objectStore.remove(OTHER_KEY)).thenReturn(value);

    wrapper = new MonitoredObjectStoreWrapper(objectStore, settings);

    wrapper.expire();

    check(5000, 100, () -> {
      verify(objectStore).remove(OTHER_KEY);
      return true;
    });
  }

  @Test
  @Issue("MULE-18579")
  public void expirationQueueDoesntHoldObjects() throws ObjectStoreException, InterruptedException {
    when(settings.getMaxEntries()).thenReturn(of(0));
    when(settings.getEntryTTL()).thenReturn(empty());

    Serializable innerValue = new Serializable() {};
    final PhantomReference<Serializable> phantomReference = new PhantomReference<>(innerValue, new ReferenceQueue<>());

    objectStore = new InMemoryObjectStore() {

      @Override
      public Serializable remove(String key) throws ObjectStoreException {
        super.remove(key);

        new PollingProber(GC_POLLING_TIMEOUT, DEFAULT_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
          System.gc();
          assertThat(phantomReference.isEnqueued(), is(true));
          return true;
        }, "A hard reference is being mantained to the value of the OS entry."));

        return new MonitoredObjectStoreWrapper.StoredObject(new Serializable() {}, 0L, KEY);
      }

      @Override
      public List allKeys() throws ObjectStoreException {
        return asList(KEY);
      }
    };

    objectStore.store(KEY, new StoredObject<>(innerValue, 0L, KEY));

    innerValue = null;

    wrapper = new MonitoredObjectStoreWrapper(objectStore, settings);
    wrapper.expire();
  }
}
