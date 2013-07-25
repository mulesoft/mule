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

import java.io.Serializable;

/**
 * 
 */
public class PagingConfiguration implements Serializable
{

    private static final long serialVersionUID = 3647804910350935195L;
    
    private boolean paging = false;
    private int pageSize = 100;

    public PagingConfiguration()
    {
    }

    public PagingConfiguration(boolean paging, int pageSize)
    {
        this();
        this.paging = paging;
        this.pageSize = pageSize;
    }

    public boolean isPaging()
    {
        return paging;
    }

    public void setPaging(boolean paging)
    {
        this.paging = paging;
    }

    public int getPageSize()
    {
        return pageSize;
    }

    public void setPageSize(int pageSize)
    {
        this.pageSize = pageSize;
    }

}
