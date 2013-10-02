/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

import static org.junit.Assert.fail;

@SmallTest
public class KeyedPoolMessageDispatcherFactoryAdapterTestCase extends AbstractMuleTestCase
{

    @Test
    public void testNullFactory()
    {
        try
        {
            new KeyedPoolMessageDispatcherFactoryAdapter(null);
            fail("expected IllegalArgumentException");
        }
        catch (IllegalArgumentException iex)
        {
            // OK
        }
    }

}
