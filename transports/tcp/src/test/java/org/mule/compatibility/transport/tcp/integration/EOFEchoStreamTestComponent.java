/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.integration;

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


