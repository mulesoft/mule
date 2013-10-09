/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.integration;

import java.io.IOException;
import java.io.InputStream;

public class EOFEchoStreamTestComponent
{
    public Object invoke(final InputStream is) throws IOException
    {
        byte[] buf = new byte[is.available()];
        int n = 0;
        int total = 0;
        while (total < 16)
        {
            n = is.read(buf);
            total += n;
        }
        is.close();
        
        return new String(buf);
    }
}


