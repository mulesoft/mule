/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.store;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class MuleObjectStoreManagerTestCase extends AbstractMuleTestCase
{

    private MuleObjectStoreManager storeManager = new MuleObjectStoreManager();

    @Test
    public void disposesStore() throws ObjectStoreException
    {
        DisposableObjectStore store = Mockito.mock(DisposableObjectStore.class);

        storeManager.disposeStore(store);

        Mockito.verify(store).dispose();
    }

    public static interface DisposableObjectStore extends ObjectStore, Disposable
    {

    }
}
