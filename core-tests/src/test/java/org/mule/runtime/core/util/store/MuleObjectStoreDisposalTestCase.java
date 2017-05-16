/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.store;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class MuleObjectStoreDisposalTestCase extends AbstractMuleContextTestCase {

  private static final String TEST_OS_NAME = "disposalTest";
  private static final int MAX_ENTRIES = 100;
  private static final int TIMEOUT = 9999;
  private static final String DISPOSABLE_TRANSIENT_USER_STORE_KEY = "DISPOSABLE_TRANSIENT_USER_STORE_KEY";

  private MuleObjectStoreManager osm;

  @Override
  protected void doSetUp() throws Exception {
    osm = muleContext.getRegistry().lookupObject(OBJECT_STORE_MANAGER);
    muleContext.getRegistry().registerObject(DISPOSABLE_TRANSIENT_USER_STORE_KEY, new SimpleMemoryObjectStore<>());
    osm.setBaseTransientUserStoreKey(DISPOSABLE_TRANSIENT_USER_STORE_KEY);
  }

  @Test
  public void shutdownScheduler() throws Exception {
    doDispose();
    assertThat(osm.scheduler.isShutdown(), is(true));
  }

  @Test
  public void disposeMonitoredObjectStores() throws Exception {
    osm.getUserObjectStore(TEST_OS_NAME, false, MAX_ENTRIES, TIMEOUT, TIMEOUT);
    ObjectStore<?> managedObjectStore = osm.stores.get(TEST_OS_NAME);

    assertNotNull(managedObjectStore);
    assertTrue(managedObjectStore instanceof Disposable);

    managedObjectStore = spy(managedObjectStore);
    osm.stores.put(TEST_OS_NAME, managedObjectStore);

    doDispose();
    verify((Disposable) managedObjectStore).dispose();
  }

  private void doDispose() {
    muleContext.dispose();
    muleContext = null;
  }
}
