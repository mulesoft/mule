/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.store;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreNotAvaliableException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.Serializable;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MonitoredObjectStoreTestCase extends AbstractMuleTestCase
{
    private static final int EXPIRATION_INTERVAL = 500;
    
    @Test
    public void testShutdownWithHangingExpireThread() throws Exception
    {        
        ExpiringStore store = createExpiringStore();
        
        // sleep some time for the expire to kick in
        Thread.sleep(EXPIRATION_INTERVAL * 2);
        
        // now dispose the store, this kills the expire thread 
        // that is still active, as it is a daemon thread
        store.dispose();
        
        assertTrue(store.expireStarted);
        assertFalse(store.expireFinished);
    }

    private ExpiringStore createExpiringStore() throws InitialisationException
    {
        ExpiringStore store = new ExpiringStore();
        store.setExpirationInterval(EXPIRATION_INTERVAL);
        store.initialise();
        
        return store;
    }
    
    private static class ExpiringStore extends AbstractMonitoredObjectStore<String>
    {
        protected boolean expireStarted = false;
        protected boolean expireFinished = false;
        
        public ExpiringStore()
        {
            super();
        }
        
        @Override
        protected void expire()
        {
            try
            {
                expireStarted = true;
                Thread.sleep(EXPIRATION_INTERVAL * 10);
                expireFinished = true;
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException("expire was interrupted", e);
            }
        }
        
        public boolean contains(Serializable id) throws ObjectStoreNotAvaliableException
        {
            return false;
        }

        public String remove(Serializable id) throws ObjectStoreException
        {
            return null;
        }

        public String retrieve(Serializable id) throws ObjectStoreException
        {
            return null;
        }

        public void store(Serializable id, String item) throws ObjectStoreException
        {
            // does nothing
        }


        public boolean isPersistent()
        {
            return false;
        }
    }
}
