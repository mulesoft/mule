/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.store;

import org.mule.api.store.ListableObjectStore;
import org.mule.tck.store.AbstractObjectStoreTestCase;

public class InMemoryObjectStoreTestCase extends AbstractObjectStoreTestCase
{
    @Override
    protected ListableObjectStore createStore()
    {
        return new InMemoryObjectStore();
    }
}
