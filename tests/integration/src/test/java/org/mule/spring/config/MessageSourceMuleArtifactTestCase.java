/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.spring.config;

import junit.framework.Assert;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.DOMWriter;
import org.junit.Test;
import org.mockito.Mockito;
import org.mule.common.MuleArtifact;
import org.mule.common.MuleArtifactFactoryException;
import org.mule.common.Testable;
import org.mule.common.config.XmlConfigurationCallback;
import org.mule.config.spring.SpringXmlConfigurationMuleArtifactFactory;
import org.w3c.dom.Element;

import java.util.HashMap;

public class MessageSourceMuleArtifactTestCase
{

    @Test
    public void whenCallingGetArtifactForMessageSource() throws MuleArtifactFactoryException, DocumentException
    {
        SpringXmlConfigurationMuleArtifactFactory factoryTest = new SpringXmlConfigurationMuleArtifactFactory();
        XmlConfigurationCallback callback = Mockito.mock(XmlConfigurationCallback.class);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("test", "test1");
        Mockito.when(callback.getEnvironmentProperties()).thenReturn(map);
        Mockito.when(callback.getPropertyPlaceholders()).thenReturn(new Element[] {});
        Mockito.when(callback.getSchemaLocation("http://www.mulesoft.org/schema/mule/vm")).thenReturn("http://www.mulesoft.org/schema/mule/vm/current/mule-vm.xsd");
        Element element = createElement("inbound-endpoint", "http://www.mulesoft.org/schema/mule/vm", "vm");
        element.setAttribute("path", "/test");


        MuleArtifact artifact = factoryTest.getArtifactForMessageProcessor(element, callback);

        Assert.assertFalse(artifact.hasCapability(Testable.class));
    }

    private static Element createElement(String name, String namespace, String prefix) throws DocumentException
    {
        org.dom4j.Element dom4jElement = DocumentHelper.createElement(new QName(name, new Namespace(prefix, namespace)));
        org.dom4j.Document document = dom4jElement.getDocument();
        if (document == null)
        {
            document = DocumentHelper.createDocument();
            document.setRootElement(dom4jElement);
        }

        final DOMWriter writer = new DOMWriter();
        org.w3c.dom.Document w3cDocument = writer.write(document);
        Element w3cElement = w3cDocument.getDocumentElement();


        return w3cElement;
    }
}
