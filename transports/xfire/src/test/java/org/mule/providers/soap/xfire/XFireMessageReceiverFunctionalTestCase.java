/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.components.simple.EchoService;
import org.mule.providers.AbstractConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.IOUtils;

import java.lang.reflect.Field;
import java.util.Map;

import javax.xml.namespace.QName;

import org.codehaus.xfire.client.XFireProxyFactory;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.binding.ObjectServiceFactory;
import org.custommonkey.xmlunit.XMLUnit;

// MULE-2608: Change XFireMessageReceiver to allow a custom portType to be converted from
// a raw string to a QName
public class XFireMessageReceiverFunctionalTestCase extends FunctionalTestCase
{
    protected String echoWsdl;

    protected String getConfigResources()
    {
        return "xfire-advanced-conf.xml";
    }

    // @Override
    protected void doSetUp() throws Exception
    {
        echoWsdl = IOUtils.getResourceAsString("xfire-advanced-echo-service.wsdl", getClass());
        XMLUnit.setIgnoreWhitespace(true);
    }

    public void testQname() throws Exception
    {
        AbstractConnector umoConnector = (AbstractConnector) managementContext.getRegistry().lookupConnector(
            "xfireConnector");
        Map map = umoConnector.getReceivers();
        XFireMessageReceiver receiver = (XFireMessageReceiver) map.get("http://localhost:63081/services/echoServiceWsdlPortType");
        Field field = XFireMessageReceiver.class.getDeclaredField("service");
        field.setAccessible(true);
        Service service = (Service) field.get(receiver);
        QName qname = (QName) service.getProperty(ObjectServiceFactory.PORT_TYPE);
        assertNotNull(qname);
        assertTrue(qname.getNamespaceURI().indexOf("echoServiceCustomPortType") > -1);
    }

    public void testExternalXFireInvocation() throws Exception
    {
        Service serviceModel = new ObjectServiceFactory().create(EchoService.class);
        EchoService echoService = (EchoService) new XFireProxyFactory().create(serviceModel,
            "http://localhost:63081/services/echoServiceWsdlPortType");
        String response = echoService.echo("hello world");
        assertEquals("hello world", response);
    }

}
