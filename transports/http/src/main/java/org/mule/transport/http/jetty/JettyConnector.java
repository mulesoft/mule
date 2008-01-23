/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.jetty;

import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpsConnector;

/**
 * <code>ServletConnector</code> is a channel adapter between Mule and a servlet
 * engine.
 */

public class JettyConnector extends HttpConnector
{

    public static final String JETTY = "jetty";
    public static final String REST = "rest";

    public JettyConnector()
    {
        super();
        registerSupportedProtocol(HttpConnector.HTTP);
        registerSupportedProtocol(HttpsConnector.HTTPS);
        registerSupportedProtocol(REST);
    }

    public String getProtocol()
    {
        return JETTY;
    }

}
