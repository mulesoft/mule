/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.store;

import java.io.Serializable;

public interface ObjectStoreManager
{
    public <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name);

    public <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name, boolean isPersistent);

    public <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name,
                                                                            boolean isPersistent,
                                                                            int maxEntries,
                                                                            int entryTTL,
                                                                            int expirationInterval);
    public void disposeStore(ObjectStore<? extends Serializable> store) throws ObjectStoreException;
}
