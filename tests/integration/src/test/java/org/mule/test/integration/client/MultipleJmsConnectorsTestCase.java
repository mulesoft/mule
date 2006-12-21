/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.client;

import org.mule.extras.client.MuleClient;
import org.mule.tck.AbstractMuleTestCase;

import org.activemq.ActiveMQConnectionFactory;
import org.activemq.broker.impl.BrokerContainerFactoryImpl;
import org.activemq.store.vm.VMPersistenceAdapter;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class MultipleJmsConnectorsTestCase extends AbstractMuleTestCase
{
    public void testMultipleJmsClientConnections() throws Exception
    {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL("vm://localhost");
        factory.setBrokerContainerFactory(new BrokerContainerFactoryImpl(new VMPersistenceAdapter()));

        MuleClient client = new MuleClient();
        client.setProperty("jms.connectionFactory", factory);
        client.setProperty("jms.specification", "1.1");
        client.getManager().start();
        client.dispatch("jms://admin:admin@admin.queue?createConnector=ALWAYS", "testing", null);
        client.dispatch("jms://ross:ross@ross.queue?createConnector=ALWAYS", "testing", null);

        assertEquals(2, client.getManager().getConnectors().size());
    }
}
