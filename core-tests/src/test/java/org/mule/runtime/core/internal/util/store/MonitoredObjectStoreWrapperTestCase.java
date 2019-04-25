/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.tck.probe.PollingProber.check;

import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.internal.util.store.MonitoredObjectStoreWrapper.StoredObject;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MonitoredObjectStoreWrapperTestCase extends AbstractMuleTestCase {

  private static final String KEY = "key";
  private static final String OTHER_KEY = "otherKey";

  @Mock
  private ObjectStoreSettings settings;

  @Mock
  private ObjectStore<StoredObject<String>> objectStore;
  private MonitoredObjectStoreWrapper<String> wrapper;

  @Test
  public void expireWithoutMaxEntries() throws Exception {
    when(settings.getMaxEntries()).thenReturn(empty());
    when(settings.getEntryTTL()).thenReturn(of(1L));

    StoredObject<String> value = new StoredObject<>("", 0L, KEY);
    when(objectStore.allKeys()).thenReturn(asList(KEY));
    when(objectStore.retrieve(KEY)).thenReturn(value);
    when(objectStore.contains(KEY)).thenReturn(true);
    when(objectStore.remove(KEY)).thenReturn(value);

    wrapper = new MonitoredObjectStoreWrapper<>(objectStore, settings);

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

    wrapper = new MonitoredObjectStoreWrapper<>(objectStore, settings);

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

    wrapper = new MonitoredObjectStoreWrapper<>(objectStore, settings);

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

    wrapper = new MonitoredObjectStoreWrapper<>(objectStore, settings);

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

    wrapper = new MonitoredObjectStoreWrapper<>(objectStore, settings);

    wrapper.expire();

    check(5000, 100, () -> {
      verify(objectStore).remove(OTHER_KEY);
      return true;
    });
  }
}
