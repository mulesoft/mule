/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MultipleCollectionAggregatorsTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "multiple-collection-aggregators-config.xml";
    }

    @Test
    public void testStartsCorrectly()
    {
        assertTrue(muleContext.isStarted());
    }
}
