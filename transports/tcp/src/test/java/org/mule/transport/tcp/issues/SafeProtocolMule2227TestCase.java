/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.issues;

import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;
import org.mule.transport.tcp.protocols.SafeProtocolTestCase;

import org.junit.Test;

public class SafeProtocolMule2227TestCase extends SafeProtocolTestCase
{

    // this actually "works" much of the time, in that a response is received that looks reasonable.
    // that's just because the test is so simple that the length encoded string is read by the
    // server as a literal chunk of text (including the cookies and lengths!).  on the return these
    // are still present so the data that were sent are read (missing the appended text).

    // the rest of the time, it gives an out of memory error.  that's because it responds to the
    // cookie alone, which means that "Received" is taken as a message, and "Re" gives a length of
    // 542270819 leading to a memory overflow

    // the only way round this i can see is to allow a parameter on the protocol to specify a
    // maximum size.  see MULE-2449.

    // update - we now do have a maximum size

    public SafeProtocolMule2227TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testSafeToUnsafe() throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);
        // this may fail, but should not crash
        try
        {
            client.send("tcp://localhost:" + dynamicPort1.getNumber() + "?connector=safe", TEST_MESSAGE, null);
        }
        catch(Exception e)
        {
            // an error is ok - we were losing the JVM before
        }
    }

}
