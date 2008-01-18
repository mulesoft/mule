/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.vm.functional;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOMessage;

public class VmTransactionTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "vm/vm-transaction.xml";
    }

    public void testTransaction() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("vm://in", "TEST", null);
        UMOMessage message = client.request("vm://out", 10000);
        assertNotNull(message);

    }

    public static class TestComponent
    {

        public Object process(Object a) throws Exception
        {
            assertNotNull(TransactionCoordination.getInstance().getTransaction());
            return a;
        }

    }

}
