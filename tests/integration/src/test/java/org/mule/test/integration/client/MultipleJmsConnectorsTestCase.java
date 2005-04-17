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
package org.mule.test.integration.client;

import org.mule.extras.client.MuleClient;
import org.mule.tck.NamedTestCase;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class MultipleJmsConnectorsTestCase extends NamedTestCase
{
    public void testMultipleJmsClientConnections() throws Exception {
        MuleClient client = new MuleClient();
        client.setProperty("jms.connectionFactoryJndiName", "ConnectionFactory");
        client.setProperty("jms.jndiInitialFactory", "org.codehaus.activemq.jndi.ActiveMQInitialContextFactory");
        client.setProperty("jms.specification", "1.1");

        client.dispatch("jms://admin:admin@admin.queue?createConnector=ALWAYS", "admin", null);
        client.dispatch("jms://ross:ross@ross.queue?createConnector=ALWAYS", "admin", null);

        assertEquals(2, client.getManager().getConnectors().size());
    }
}
