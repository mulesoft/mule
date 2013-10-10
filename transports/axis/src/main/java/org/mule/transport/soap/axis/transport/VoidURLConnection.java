/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    public void connect()
    {
        // nothing to do
    }

}
