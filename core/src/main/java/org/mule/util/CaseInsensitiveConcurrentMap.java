/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A case insensitive map based on a concurrent hash map.
 *
 * @since 3.6.0
 */
public class CaseInsensitiveConcurrentMap<T> extends CaseInsensitiveMapWrapper<T>
{

    public CaseInsensitiveConcurrentMap() throws Exception
    {
        super(ConcurrentHashMap.class);
    }
}
