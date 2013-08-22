/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming;

/**
 * Inmutable pojo to carry pagination parameters
 */
public class PagingConfiguration
{

    /**
     * The amount of items to fetch on each invocation to the data source
     */
    private int fetchSize;
    
    /**
     * Zero-based index of the first page to be returned
     */
    private int firstPage;
    
    /**
     * Zero-based index of the top page to be returned. -1 means do not limit
     */
    private int lastPage;
    
    /**
     * Specifies whether to iterate through each individual element or whole pages of fetchSize elements
     */
    private StreamingOutputUnit outputUnit;

    public PagingConfiguration(int fetchSize, int firstPage, int lastPage, StreamingOutputUnit outputUnit)
    {
        this.fetchSize = fetchSize;
        this.firstPage = firstPage;
        this.lastPage = lastPage;
        this.outputUnit = outputUnit;
    }

    public int getFetchSize()
    {
        return fetchSize;
    }

    public int getFirstPage()
    {
        return firstPage;
    }

    public int getLastPage()
    {
        return lastPage;
    }
    
    public StreamingOutputUnit getOutputUnit()
    {
        return outputUnit;
    }

}
