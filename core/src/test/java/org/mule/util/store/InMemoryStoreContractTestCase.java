/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
