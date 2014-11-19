/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.jms;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.xa.DefaultXAConnectionFactoryWrapper;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class JmsXaSharedConnectorTestCase extends JmsSharedConnectorTestCase
{

    public JmsXaSharedConnectorTestCase(String domainConfig)
    {
        super(domainConfig);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"domain/jms/jms-activemq-xa-embedded-shared-connector.xml"}
        });
    }

    @Test
    public void connectionFactoryWrapperIsUsed()
    {
        JmsConnector jmsConnector = (JmsConnector) getMuleContextForApp(CLIENT_APP).getRegistry().lookupConnector("sharedJmsConnector");
        assertThat(jmsConnector.getConnectionFactory(), instanceOf(DefaultXAConnectionFactoryWrapper.class));
    }

}
