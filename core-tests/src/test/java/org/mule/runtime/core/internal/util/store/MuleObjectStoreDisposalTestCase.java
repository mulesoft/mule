/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MuleObjectStoreDisposalTestCase extends AbstractMuleContextTestCase {

  private static final String TEST_OS_NAME = "disposalTest";
  private static final int MAX_ENTRIES = 100;
  private static final long TIMEOUT = 9999;
  private static final String DISPOSABLE_TRANSIENT_USER_STORE_KEY = "DISPOSABLE_TRANSIENT_USER_STORE_KEY";

  private MuleObjectStoreManager osm;

  @Mock(extraInterfaces = Disposable.class)
  private ObjectStore disposableStore;

  @Override
  protected void doSetUp() throws Exception {
    osm = ((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(OBJECT_STORE_MANAGER);
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(DISPOSABLE_TRANSIENT_USER_STORE_KEY, disposableStore);
    osm.setBaseTransientStoreKey(DISPOSABLE_TRANSIENT_USER_STORE_KEY);
  }

  @Test
  public void disposeMonitoredObjectStores() throws Exception {
    ObjectStore<?> managedObjectStore = osm.createObjectStore(TEST_OS_NAME, ObjectStoreSettings.builder()
        .persistent(false)
        .maxEntries(MAX_ENTRIES)
        .entryTtl(TIMEOUT)
        .expirationInterval(TIMEOUT)
        .build());

    assertNotNull(managedObjectStore);
    assertTrue(managedObjectStore instanceof Disposable);

    doDispose();
    verify((Disposable) disposableStore).dispose();
  }

  private void doDispose() {
    muleContext.dispose();
    muleContext = null;
  }
}
