/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.ssl;

import com.mockobjects.dynamic.Mock;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.AbstractConnector;
import org.mule.tck.providers.AbstractMessageReceiverTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class SslMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    /**
     * Store created using
     * C:\dev\projects\mule\src\providers\ssl\src\test\conf>keytool -genkey -keystore serverKeystore -keyalg rsa -alias muleserver -storepass mulepassword -keypass mulepassword
     *
     */
    public UMOMessageReceiver getMessageReceiver() throws Exception
    {
        endpoint = new MuleEndpoint("ssl://localhost:10101", true);
        SslConnector cnn = SslConnectorTestCase.createConnector(true);
        endpoint.setConnector(cnn);
        Mock mockComponent = new Mock(UMOComponent.class);

        return new SslMessageReceiver((AbstractConnector) endpoint.getConnector(), (UMOComponent) mockComponent.proxy(), endpoint);
    }
}
