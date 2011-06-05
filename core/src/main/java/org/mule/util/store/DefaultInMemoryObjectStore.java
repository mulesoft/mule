/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.config.MuleProperties;

import java.io.Serializable;

/**
 * A facade for the default in-memory object store
 */
public class DefaultInMemoryObjectStore<T extends Serializable> extends FacadeObjectStore<T>
{
    public DefaultInMemoryObjectStore()
    {
        super(MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME);
    }
}
