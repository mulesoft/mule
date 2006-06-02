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
package org.mule.providers.jbi;

import org.mule.providers.jbi.components.MuleReceiver;
import org.mule.tck.AbstractMuleTestCase;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

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

import java.util.MissingResourceException;
import java.util.logging.Logger;

/**
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ReceiverComponentTestCase extends AbstractMuleTestCase
{
    public void testX() throws Exception {
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

    class DummyComponentContext implements ComponentContext {
        public ServiceEndpoint activateEndpoint(QName qName, String string) throws JBIException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void deactivateEndpoint(ServiceEndpoint serviceEndpoint) throws JBIException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void registerExternalEndpoint(ServiceEndpoint serviceEndpoint) throws JBIException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void deregisterExternalEndpoint(ServiceEndpoint serviceEndpoint) throws JBIException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public ServiceEndpoint resolveEndpointReference(DocumentFragment documentFragment) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getComponentName() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public DeliveryChannel getDeliveryChannel() throws MessagingException {
            return new DummyDeliveryChannel() ;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public ServiceEndpoint getEndpoint(QName qName, String string) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Document getEndpointDescriptor(ServiceEndpoint serviceEndpoint) throws JBIException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public ServiceEndpoint[] getEndpoints(QName qName) {
            return new ServiceEndpoint[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        public ServiceEndpoint[] getEndpointsForService(QName qName) {
            return new ServiceEndpoint[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        public ServiceEndpoint[] getExternalEndpoints(QName qName) {
            return new ServiceEndpoint[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        public ServiceEndpoint[] getExternalEndpointsForService(QName qName) {
            return new ServiceEndpoint[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getInstallRoot() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Logger getLogger(String string, String string1) throws MissingResourceException, JBIException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public MBeanNames getMBeanNames() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public MBeanServer getMBeanServer() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public InitialContext getNamingContext() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object getTransactionManager() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getWorkspaceRoot() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    class DummyDeliveryChannel implements DeliveryChannel {
        public void close() throws MessagingException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public MessageExchangeFactory createExchangeFactory() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public MessageExchangeFactory createExchangeFactory(QName qName) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public MessageExchangeFactory createExchangeFactoryForService(QName qName) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public MessageExchangeFactory createExchangeFactory(ServiceEndpoint serviceEndpoint) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public MessageExchange accept() throws MessagingException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public MessageExchange accept(long l) throws MessagingException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void send(MessageExchange messageExchange) throws MessagingException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean sendSync(MessageExchange messageExchange) throws MessagingException {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean sendSync(MessageExchange messageExchange, long l) throws MessagingException {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
