/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreNotAvailableException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.Serializable;

import org.junit.Test;

public class MonitoredObjectStoreTestCase extends AbstractMuleContextTestCase {

  private static final int EXPIRATION_INTERVAL = 500;

  @Test
  public void testShutdownWithHangingExpireThread() throws Exception {
    ExpiringStore store = createExpiringStore();

    // sleep some time for the expire to kick in
    Thread.sleep(EXPIRATION_INTERVAL * 2);

    // now dispose the store, this kills the expire thread
    // that is still active, as it is a daemon thread
    store.dispose();

    assertTrue(store.expireStarted);
    assertFalse(store.expireFinished);
  }

  private ExpiringStore createExpiringStore() throws InitialisationException {
    ExpiringStore store = new ExpiringStore();
    store.setMuleContext(muleContext);
    store.setExpirationInterval(EXPIRATION_INTERVAL);
    store.initialise();

    return store;
  }

  private static class ExpiringStore extends AbstractMonitoredObjectStore<String> {

    boolean expireStarted = false;
    boolean expireFinished = false;

    public ExpiringStore() {
      super();
    }

    @Override
    protected void expire() {
      try {
        expireStarted = true;
        Thread.sleep(EXPIRATION_INTERVAL * 10);
        expireFinished = true;
      } catch (InterruptedException e) {
        throw new RuntimeException("expire was interrupted", e);
      }
    }

    @Override
    public boolean contains(Serializable id) throws ObjectStoreNotAvailableException {
      return false;
    }

    @Override
    public String remove(Serializable id) throws ObjectStoreException {
      return null;
    }

    @Override
    public String retrieve(Serializable id) throws ObjectStoreException {
      return null;
    }

    @Override
    public void store(Serializable id, String item) throws ObjectStoreException {
      // does nothing
    }

    @Override
    public void clear() {
      // does nothing
    }

    @Override
    public boolean isPersistent() {
      return false;
    }
  }
}
