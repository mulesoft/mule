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

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPagingConsumerTestCase
{

    protected static final int totalCount = 50;
    protected static final int pageSize = 10;

    protected List<List<Integer>> pages;

    protected List<List<Integer>> getPages()
    {
        List<List<Integer>> pages = new ArrayList<List<Integer>>();
        List<Integer> page = new ArrayList<Integer>();

        for (int i = 1; i <= totalCount; i++)
        {
            page.add(i);
            if (i % pageSize == 0)
            {
                pages.add(page);
                page = new ArrayList<Integer>();
            }
        }

        return pages;
    }

}
