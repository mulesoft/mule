/*
 * $Header$ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved. http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *
 */

package org.mule.providers.jms;


import org.mule.providers.jms.support.JmsTestUtils;
import org.mule.tck.providers.AbstractTransactionEnabledConnectorTestCase;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.ObjectFactory;

import javax.naming.InitialContext;
import java.util.HashMap;
import java.util.Properties;


/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class JmsConnectorTestCase extends AbstractTransactionEnabledConnectorTestCase
{
    private JmsConnector connector;

    /*
	 * (non-Javadoc)
	 *
	 * @see org.mule.tck.providers.AbstractConnectorTestCase#getConnectorName()
	 */
    public UMOConnector getConnector() throws Exception
    {
        if (connector == null)
        {
            Properties jndi = JmsTestUtils.getJmsProperties();
            connector = new JmsConnector();
            connector.setName("TestConnector");
            connector.setProviderProperties(new HashMap(jndi));
            connector.setConnectionFactoryJndiName(jndi.getProperty("connectionFactoryJNDIName"));
            connector.initialise();
        }
        return connector;
    }

    public String getTestEndpointURI()
    {
        return "jms://test.queue";
    }

    public Object getValidMessage() throws Exception
    {
        return JmsTestUtils.getTextMessage(JmsTestUtils.getQueueConnection(), "Test JMS Message");
    }

    public static class ConnectionFactoryFactory implements ObjectFactory
    {
        public Object create() throws Exception
        {
            Properties p = JmsTestUtils.getJmsProperties();
            InitialContext ctx = new InitialContext(p);
            return ctx.lookup(p.getProperty("connectionFactoryJNDIName"));
        }
    }

}