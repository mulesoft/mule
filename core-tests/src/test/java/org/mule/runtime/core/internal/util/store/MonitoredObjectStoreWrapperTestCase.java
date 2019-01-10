/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.internal.util.store.MonitoredObjectStoreWrapper.StoredObject;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MonitoredObjectStoreWrapperTestCase extends AbstractMuleTestCase {

  private static final String KEY = "key";

  @Mock
  private ObjectStoreSettings settings;

  @Mock
  private ObjectStore objectStore;

  private MonitoredObjectStoreWrapper wrapper;

  @Before
  public void before() throws Exception {
    StoredObject value = new StoredObject("", 0L, KEY);
    when(objectStore.retrieve(KEY)).thenReturn(value);
    when(objectStore.allKeys()).thenReturn(asList(value));
  }

  @Test
  public void expireWithoutMaxEntries() {
    when(settings.getMaxEntries()).thenReturn(empty());
    when(settings.getEntryTTL()).thenReturn(of(500L));
    wrapper = new MonitoredObjectStoreWrapper(objectStore, settings);

    wrapper.expire();
  }


}
