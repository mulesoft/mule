/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.http.functional;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.http.jetty.JettyConnector;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import java.net.URI;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;


public class JettyRestFunctionalTestCase extends HttpFunctionalTestCase
{
    //@Override
    protected String getConfigResources()
    {
        return "jetty-rest-functional-test.xml";
    }
}
