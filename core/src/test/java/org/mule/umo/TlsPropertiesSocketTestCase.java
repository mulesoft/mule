/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.security.tls.TlsConfiguration;
import org.mule.umo.security.tls.TlsPropertiesSocketFactory;

public class TlsPropertiesSocketTestCase extends AbstractMuleTestCase
{

    public void testSimpleSocket() throws Exception
    {
        TlsConfiguration configuration = new TlsConfiguration(TlsConfiguration.DEFAULT_KEYSTORE);
        configuration.setKeyPassword("mulepassword");
        configuration.setKeyStorePassword("mulepassword");
        configuration.setKeyStore("clientKeystore");
        configuration.initialise(false, TlsConfiguration.JSSE_NAMESPACE);

        TlsPropertiesSocketFactory socketFactory = 
            new TlsPropertiesSocketFactory(true, TlsConfiguration.JSSE_NAMESPACE);
        assertTrue("socket is useless", socketFactory.getSupportedCipherSuites().length > 0);
    }

}


