/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.streaming;

/**
 * 
 */
public class PagingConfiguration
{

    private int fetchSize;
    private int firstPage;
    private int lastPage;
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
