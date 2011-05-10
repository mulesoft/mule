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

/**
 * A facade for the default simple memory-based object store
 */
public class MemoryObjectStore extends FacadeObjectStore
{
    public MemoryObjectStore()
    {
        super(MuleProperties.OBJECT_STORE_SIMPLE_MEMORY_NAME);
    }
}