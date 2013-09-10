/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.store.ObjectStore;

import java.io.Serializable;

public class TextFileObjectStoreContractTestCase extends AbstractObjectStoreContractTestCase
{
    private TextFileObjectStore objectStore;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        objectStore = new TextFileObjectStore();
        objectStore.setMuleContext(muleContext);
        objectStore.initialise();
    }
    
    @Override
    protected void doTearDown() throws Exception
    {
        objectStore.dispose();
        super.doTearDown();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ObjectStore getObjectStore()
    {
        return objectStore;
    }

    @Override
    public Serializable getStorableValue()
    {
        return "This is the value";
    }
}


