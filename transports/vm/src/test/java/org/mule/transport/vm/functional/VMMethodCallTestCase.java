/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class VMMethodCallTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "vm/vm-method-call-test.xml";
    }

    public void testCall() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm://methodTest?method=method2&connector=vm1", "dummy msg", null);
        String result = (String) reply.getPayload();
        assertTrue(result.equals("method2 called"));
    }

    public static class MethodTestComponent
    {
        public String method1(String s) { return "method1 called"; }

        public String method2(String s) { return "method2 called"; }
    }
}
