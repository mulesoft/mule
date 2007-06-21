/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.issues;

import org.mule.extras.client.MuleClient;
import org.mule.providers.AbstractFunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

public abstract class AbstractMethodTypeMule1857TestCase extends AbstractFunctionalTestCase
{

    public AbstractMethodTypeMule1857TestCase(String prefix, String config)
    {
        super(prefix, config);
    }

    public void testBadMethodType() throws Exception
    {
        try
        {
            new MuleClient().send("BadType", "hello", null);
            fail("expected error");
        }
        catch (UMOException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    public void testCorrectMethodType() throws Exception
    {
        UMOMessage message = new MuleClient().send("GoodType", "hello", null);
        assertNotNull(message);
        assertEquals("olleh", message.getPayloadAsString());
    }

}
