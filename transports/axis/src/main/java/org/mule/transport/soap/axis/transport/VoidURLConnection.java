/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.transport;

import java.net.URL;

/**
 * A fake url connection used to bypass Axis's use of the URLStreamHandler to mask
 * uris as Urls. This was also necessary because of the uncessary use of static
 * blocking in the axis URLStreamHandler objects.
 */
public class VoidURLConnection extends java.net.URLConnection
{
    public VoidURLConnection(URL url)
    {
        super(url);
    }

    @Override
    public void connect()
    {
        // nothing to do
    }
}
