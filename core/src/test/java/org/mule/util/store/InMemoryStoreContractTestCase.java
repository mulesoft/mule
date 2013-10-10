/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.store;

import org.mule.api.store.ObjectStore;
import org.mule.transport.NullPayload;

import java.io.Serializable;

public class InMemoryStoreContractTestCase extends AbstractObjectStoreContractTestCase
{
    @Override
    public ObjectStore<Serializable> getObjectStore()
    {
        return new InMemoryObjectStore<Serializable>();
    }

    @Override
    public Serializable getStorableValue()
    {
        return NullPayload.getInstance();
    }
}
