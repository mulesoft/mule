/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.net.ConnectException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.fail;

public class HttpsInvalidKeystoreTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");


    @Override
    protected String getConfigResources()
    {
        return "https-invalid-keystore-config.xml";
    }

    @Test(expected=ConnectException.class)
    public void messageReceiverIsNotStartedWithMissingKeystore() throws Exception
    {
        String url = String.format("https://localhost:%1d/?name=Ross", port1.getNumber());
        GetMethod getMethod = new GetMethod(url);
        new HttpClient().executeMethod(getMethod);
        fail("The https message receiver may not have been started without a proper keystore config");
    }
}


