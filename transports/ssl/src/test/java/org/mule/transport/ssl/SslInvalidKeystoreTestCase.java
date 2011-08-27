/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ssl;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.fail;

@Ignore
public class SslInvalidKeystoreTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "ssl-missing-keystore-config.xml";
    }

    @Test(expected=ConnectException.class)
    public void sslMessageReceiverIsNotStartedWhenKeystoreIsMissing() throws Exception
    {
        Socket socket = SSLSocketFactory.getDefault().createSocket();
        socket.connect(new InetSocketAddress("localhost", port1.getNumber()));
        socket.getOutputStream().write(TEST_MESSAGE.getBytes());
        fail("The ssl message receiver may not have been started without a proper keystore config");
    }
}
