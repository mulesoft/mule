/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.issues;

import org.mule.extras.client.MuleClient;
import org.mule.providers.tcp.protocols.SafeProtocolTestCase;
import org.mule.umo.UMOException;

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

    public void testSafeToUnsafe() throws UMOException
    {
        MuleClient client = new MuleClient();
        // this may fail, but should not crash
        client.send("tcp://localhost:65433?connector=safe", TEST_MESSAGE, null);
    }

}