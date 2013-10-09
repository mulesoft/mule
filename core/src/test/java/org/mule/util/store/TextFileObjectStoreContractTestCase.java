/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


