/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.store;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.AbstractMuleTestCase;

public class MonitoredObjectStoreTestCase extends AbstractMuleTestCase
{
    private static final int EXPIRATION_INTERVAL = 500;
    
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
    
    private static class ExpiringStore extends AbstractMonitoredObjectStore
    {
        protected boolean expireStarted = false;
        protected boolean expireFinished = false;
        
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
        
        public boolean containsObject(String id) throws Exception
        {
            return false;
        }

        public boolean removeObject(String id) throws Exception
        {
            return false;
        }

        public Object retrieveObject(String id) throws Exception
        {
            return null;
        }

        public boolean storeObject(String id, Object item) throws Exception
        {
            return false;
        }
    }
}
