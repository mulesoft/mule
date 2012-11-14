/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextFactory;
import org.mule.common.MuleArtifact;
import org.mule.common.MuleArtifactFactoryException;
import org.mule.common.config.XmlConfigurationCallback;
import org.mule.common.config.XmlConfigurationMuleArtifactFactory;
import org.mule.config.ConfigResource;
import org.mule.context.DefaultMuleContextFactory;

import java.io.StringBufferInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.DOMReader;

public class SpringXmlConfigurationMuleArtifactFactory implements XmlConfigurationMuleArtifactFactory
{
	
    @Override
    public MuleArtifact getArtifact(org.w3c.dom.Element element, XmlConfigurationCallback callback)
        throws MuleArtifactFactoryException
    {

        Document document = DocumentHelper.createDocument();
        Element rootElement = document.addElement("mule", "http://www.mulesoft.org/schema/mule/core");
        try
        {
            rootElement.add(convert(element));
            addSchemaLocation(rootElement, element, callback);
            for (int i = 0; i < element.getAttributes().getLength(); i++)
            {
                String attributeName = element.getAttributes().item(i).getLocalName();
                if (attributeName != null && attributeName.endsWith("-ref"))
                {
                    org.w3c.dom.Element depenedantElement = callback.getGlobalElement(element.getAttributes()
                        .item(i)
                        .getNodeValue());
                    if (depenedantElement != null)
                    {
                    	// if the element is a spring bean, wrap the element in a top-level spring beans element
                    	if ("http://www.springframework.org/schema/beans".equals(depenedantElement.getNamespaceURI()))
                    	{
                    		String namespaceUri = depenedantElement.getNamespaceURI();
                    		Namespace namespace = new Namespace(depenedantElement.getPrefix(), namespaceUri);
                    		Element beans = rootElement.element(new QName("beans", namespace));
                    		if (beans == null)
                    		{
                    			beans = rootElement.addElement("beans", namespaceUri);
                    		}
                    		beans.add(convert(depenedantElement));
                    	}
                    	else
                    	{
	                        rootElement.add(convert(depenedantElement));
	                        addSchemaLocation(rootElement, depenedantElement, callback);
                    	}
                    }
                    else
                    {
                        throw new MuleArtifactFactoryException("Missing dependent xml element "
                                                               + element.getAttributes()
                                                                   .item(i)
                                                                   .getLocalName());
                    }
                }
            }

            ConfigResource config = new ConfigResource("", new StringBufferInputStream(document.asXML()));
            MuleContextFactory factory = new DefaultMuleContextFactory();
            MuleContext muleContext = factory.createMuleContext(new SpringXmlConfigurationBuilder(
                new ConfigResource[]{config}));
            return new DefaultMuleArtifact(muleContext.getRegistry().lookupObject(
                element.getAttribute("name")));
        }
        catch (Exception e)
        {
            throw new MuleArtifactFactoryException("Error parsing XML", e);
        }

    }

    protected void addSchemaLocation(Element rootElement,
                                     org.w3c.dom.Element element,
                                     XmlConfigurationCallback callback)
    {
        StringBuffer schemaLocation = new StringBuffer();
        schemaLocation.append("http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\n");
        schemaLocation.append(element.getNamespaceURI() + " "
                              + callback.getSchemaLocation(element.getNamespaceURI()));
        rootElement.addAttribute(
            org.dom4j.QName.get("schemaLocation", "xsi", "http://www.w3.org/2001/XMLSchema-instance"),
            schemaLocation.toString());
    }

    /**
     * Convert w3c element to dom4j element
     * 
     * @throws ParserConfigurationException
     **/
    public org.dom4j.Element convert(org.w3c.dom.Element element) throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        org.w3c.dom.Document doc1 = builder.newDocument();
        org.w3c.dom.Element importedElement = (org.w3c.dom.Element) doc1.importNode(element, Boolean.TRUE);
        doc1.appendChild(importedElement);

        // Convert w3c document to dom4j document
        DOMReader reader = new DOMReader();
        org.dom4j.Document doc2 = reader.read(doc1);

        return doc2.getRootElement();
    }

}
