/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming;

/**
 * Inmutable pojo to carry pagination parameters
 * 
 * @since 3.5.0
 */
public class PagingConfiguration
{

    /**
     * The amount of items to fetch on each invocation to the data source
     */
    private int fetchSize;

    public PagingConfiguration(int fetchSize)
    {
        this.fetchSize = fetchSize;
    }

    public int getFetchSize()
    {
        return fetchSize;
    }

}
