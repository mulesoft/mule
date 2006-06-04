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
package org.mule.extras.pgp;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.security.UnauthorisedException;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

/**
 * @author ariva
 * 
 */
public class PGPSecurityFilterTestCase extends FunctionalTestCase
{

    protected String getConfigResources() {
        return "test-pgp-encrypt-config.xml";
    }

    public void testAuthenticationAuthorised() throws Exception
    {
        URL url = Thread.currentThread().getContextClassLoader().getResource("./encrypted-signed.asc");

        int length = (int) new File(url.getFile()).length();
        byte[] msg = new byte[length];

        FileInputStream in = new FileInputStream(url.getFile());
        in.read(msg);
        in.close();

        MuleClient client = new MuleClient();
        client.send("vm://localhost/echo", new String(msg), null);
    }

    public void testAuthenticationNotAuthorised() throws Exception
    {
        try {
            MuleClient client = new MuleClient();
            client.send("vm://localhost/echo", new String("An unsigned message"), null);
            fail("The request is not signed");
        } catch (UnauthorisedException e) {
            // ignore
        }
    }
}
