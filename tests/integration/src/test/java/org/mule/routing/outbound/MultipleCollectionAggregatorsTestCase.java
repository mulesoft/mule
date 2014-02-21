/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class MultipleCollectionAggregatorsTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "multiple-collection-aggregators-config-flow.xml";
    }

    @Test
    public void testStartsCorrectly()
    {
        assertTrue(muleContext.isStarted());
    }
}
