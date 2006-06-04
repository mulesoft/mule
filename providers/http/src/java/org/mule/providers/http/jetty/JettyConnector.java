/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.http.jetty;

import org.mule.providers.http.HttpConnector;

/**
 * <code>ServletConnector</code> is a channel adapter between Mule and a
 * servlet engine.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class JettyConnector extends HttpConnector
{
    public JettyConnector()
    {
        super();
        registerSupportedProtocol("http");
        registerSupportedProtocol("https");
        registerSupportedProtocol("rest");
    }

    public String getProtocol()
    {
        return "jetty";
    }

}
