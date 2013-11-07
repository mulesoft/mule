/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.util;

import org.mule.common.MuleArtifactFactoryException;
import org.mule.common.config.XmlConfigurationCallback;
import org.mule.config.spring.SpringXmlConfigurationMuleArtifactFactory;

import java.util.HashMap;
import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.DOMWriter;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Element;

public class SpringXmlConfigurationMuleArtifactFactoryTest
{

    @Test
    public void whenCallingGetArtifactForMessageProcessorSystemPropertiesShouldRemain() throws MuleArtifactFactoryException, DocumentException
    {
        Properties properties = System.getProperties();

        SpringXmlConfigurationMuleArtifactFactory factoryTest = new SpringXmlConfigurationMuleArtifactFactory();
        XmlConfigurationCallback callback = Mockito.mock(XmlConfigurationCallback.class);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("test", "test1");
        Mockito.when(callback.getEnvironmentProperties()).thenReturn(map);
        Mockito.when(callback.getPropertyPlaceholders()).thenReturn(new Element[] {});
        Mockito.when(callback.getSchemaLocation("http://www.mulesoft.org/schema/mule/core")).thenReturn("http://www.mulesoft.org/schema/mule/core/current/mule.xsd");
        Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");


        factoryTest.getArtifactForMessageProcessor(element, callback);

        Assert.assertThat("System properties where modified", properties, CoreMatchers.is(System.getProperties()));
    }


    public static Element createElement(String name, String namespace) throws DocumentException
    {
        org.dom4j.Element dom4jElement = DocumentHelper.createElement(new QName(name, new Namespace(null, namespace)));
        Document document = dom4jElement.getDocument();
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
