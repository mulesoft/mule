/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.transport;

import java.net.URL;

/**
 * A fake url connection used to bypass Axis's use of the URLStreamHandler to mask
 * uris as Urls. This was also necessary because of the uncessary use of static
 * blocking in the axis URLStreamHandler objects.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
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
