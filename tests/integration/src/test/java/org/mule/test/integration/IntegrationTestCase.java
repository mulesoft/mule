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
package org.mule.test.integration;

import org.activemq.ActiveMQConnectionFactory;
import org.mule.MuleManager;
import org.mule.tck.FunctionalTestCase;

import javax.jms.JMSException;

/**
 * An integration testcase also provides support for Running external embedded servers
 * such as Jms, ftp or smtp
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class IntegrationTestCase extends FunctionalTestCase
{
    protected boolean embbededActiveMQ = false;
    protected ActiveMQConnectionFactory embeddedFactory;
    protected final void doPreFunctionalSetUp() throws Exception {
        doPreIntegrationSetUp();
        if(embbededActiveMQ) {
                embeddedFactory = new ActiveMQConnectionFactory();
                embeddedFactory.setUseEmbeddedBroker(true);
                embeddedFactory.start();

            MuleManager.getInstance().setProperty("jms.connectionFactory", embeddedFactory);
            MuleManager.getInstance().setProperty("jms.specification", "1.1");
        }
    }

    protected final void doPostFunctionalSetUp() throws Exception {

        doPostIntegrationSetUp();
    }

    protected final void doFunctionalTearDown() throws Exception {
        doIntegrationTearDown();
        if(embeddedFactory!=null) {
            try {
                embeddedFactory.stop();
            } catch (JMSException e) {

            }
       }


    }

    protected void doIntegrationTearDown() throws Exception {

    }

    protected void doPreIntegrationSetUp() throws Exception {

    }

    protected void doPostIntegrationSetUp() throws Exception {

    }
}
