/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jbi;

import org.mule.providers.jbi.components.MuleReceiver;
import org.mule.tck.AbstractMuleTestCase;

import java.util.MissingResourceException;
import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.management.MBeanNames;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.MBeanServer;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

public class ReceiverComponentTestCase extends AbstractMuleTestCase
{

    public void testX() throws Exception
    {
        ComponentContext ctx = new DummyComponentContext();
        MuleReceiver receiver = new MuleReceiver();
        receiver.setEndpoint("test://foo:sender");
        receiver.setTargetService(new QName("foo:sender"));
        receiver.setName("foo:sender:http://my.org/foo");
        receiver.init(ctx);
        assertNotNull(receiver.getReceiver());
        assertNotNull(receiver.getService());
        assertEquals("foo", receiver.getService().getPrefix());
        assertEquals("sender", receiver.getService().getLocalPart());
        assertEquals("http://my.org/foo", receiver.getService().getNamespaceURI());
    }

    class DummyComponentContext implements ComponentContext
    {
        public ServiceEndpoint activateEndpoint(QName qName, String string) throws JBIException
        {
            return null;
        }

        public void deactivateEndpoint(ServiceEndpoint serviceEndpoint) throws JBIException
        {
            // nothing to do
        }

        public void registerExternalEndpoint(ServiceEndpoint serviceEndpoint) throws JBIException
        {
            // nothing to do
        }

        public void deregisterExternalEndpoint(ServiceEndpoint serviceEndpoint) throws JBIException
        {
            // nothing to do
        }

        public ServiceEndpoint resolveEndpointReference(DocumentFragment documentFragment)
        {
            return null;
        }

        public String getComponentName()
        {
            return null;
        }

        public DeliveryChannel getDeliveryChannel() throws MessagingException
        {
            return new DummyDeliveryChannel();
        }

        public ServiceEndpoint getEndpoint(QName qName, String string)
        {
            return null;
        }

        public Document getEndpointDescriptor(ServiceEndpoint serviceEndpoint) throws JBIException
        {
            return null;
        }

        public ServiceEndpoint[] getEndpoints(QName qName)
        {
            return new ServiceEndpoint[0];
        }

        public ServiceEndpoint[] getEndpointsForService(QName qName)
        {
            return new ServiceEndpoint[0];
        }

        public ServiceEndpoint[] getExternalEndpoints(QName qName)
        {
            return new ServiceEndpoint[0];
        }

        public ServiceEndpoint[] getExternalEndpointsForService(QName qName)
        {
            return new ServiceEndpoint[0];
        }

        public String getInstallRoot()
        {
            return null;
        }

        public Logger getLogger(String string, String string1) throws MissingResourceException, JBIException
        {
            return null;
        }

        public MBeanNames getMBeanNames()
        {
            return null;
        }

        public MBeanServer getMBeanServer()
        {
            return null;
        }

        public InitialContext getNamingContext()
        {
            return null;
        }

        public Object getTransactionManager()
        {
            return null;
        }

        public String getWorkspaceRoot()
        {
            return null;
        }
    }

    class DummyDeliveryChannel implements DeliveryChannel
    {
        public void close() throws MessagingException
        {
            // nothing to do
        }

        public MessageExchangeFactory createExchangeFactory()
        {
            return null;
        }

        public MessageExchangeFactory createExchangeFactory(QName qName)
        {
            return null;
        }

        public MessageExchangeFactory createExchangeFactoryForService(QName qName)
        {
            return null;
        }

        public MessageExchangeFactory createExchangeFactory(ServiceEndpoint serviceEndpoint)
        {
            return null;
        }

        public MessageExchange accept() throws MessagingException
        {
            return null;
        }

        public MessageExchange accept(long l) throws MessagingException
        {
            return null;
        }

        public void send(MessageExchange messageExchange) throws MessagingException
        {
            // nothing to do
        }

        public boolean sendSync(MessageExchange messageExchange) throws MessagingException
        {
            return false;
        }

        public boolean sendSync(MessageExchange messageExchange, long l) throws MessagingException
        {
            return false;
        }
    }

}
