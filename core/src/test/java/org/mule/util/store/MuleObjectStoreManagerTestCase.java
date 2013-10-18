/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
