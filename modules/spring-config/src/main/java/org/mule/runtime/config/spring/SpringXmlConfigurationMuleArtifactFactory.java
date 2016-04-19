/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import org.mule.common.MuleArtifact;
import org.mule.common.MuleArtifactFactoryException;
import org.mule.common.config.XmlConfigurationCallback;
import org.mule.common.config.XmlConfigurationMuleArtifactFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.config.ConfigResource;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.StringUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.DOMReader;
import org.w3c.dom.Node;

public class SpringXmlConfigurationMuleArtifactFactory implements XmlConfigurationMuleArtifactFactory
{

    public static final String BEANS_ELEMENT = "beans";

    public static final String REFS_SUFFIX = "-refs";
    public static final String REFS_TOKENS = " \t"; // Space, Tab
    public static final String REF_SUFFIX = "-ref";
    public static final String REF_ATTRIBUTE_NAME = "ref";
    public static final String NAME_ATTRIBUTE_NAME = "name";

    private Map<MuleArtifact, SpringXmlConfigurationBuilder> builders = new HashMap<MuleArtifact, SpringXmlConfigurationBuilder>();
    private Map<MuleArtifact, MuleContext> contexts = new HashMap<MuleArtifact, MuleContext>();

    @Override
    public MuleArtifact getArtifact(org.w3c.dom.Element element, XmlConfigurationCallback callback)
            throws MuleArtifactFactoryException
    {
        return doGetArtifact(element, callback, false);
    }

    @Override
    public MuleArtifact getArtifactForMessageProcessor(org.w3c.dom.Element element, XmlConfigurationCallback callback)
            throws MuleArtifactFactoryException
    {
        return doGetArtifact(element, callback, true);
    }

    protected String getArtifactMuleConfig(String flowName, org.w3c.dom.Element element, final XmlConfigurationCallback callback, boolean embedInFlow) throws MuleArtifactFactoryException
    {
        Map<String, String> schemaLocations = new HashMap<String, String>();
        schemaLocations.put("http://www.mulesoft.org/schema/mule/core", "http://www.mulesoft.org/schema/mule/core/current/mule.xsd");

        Document document = DocumentHelper.createDocument();

        // the rootElement is the root of the document
        Element rootElement = document.addElement("mule", "http://www.mulesoft.org/schema/mule/core");

        org.w3c.dom.Element[] placeholders = callback.getPropertyPlaceholders();

        for (org.w3c.dom.Element placeholder : placeholders)
        {
            try
            {
                Element newPlaceholder = convert(placeholder);
                rootElement.add(newPlaceholder);
                addSchemaLocation(placeholder, callback, schemaLocations);
            }
            catch (ParserConfigurationException e)
            {
                throw new MuleArtifactFactoryException("Error parsing XML", e);
            }

        }

        // the parentElement is the parent of the element we are adding
        Element parentElement = rootElement;
        addSchemaLocation(element, callback, schemaLocations);
        if (embedInFlow)
        {
            // Need to put the message processor in a valid flow. Our default flow is:
            //            "<flow name=\"CreateSingle\">"
            //          + "</flow>"
            parentElement = rootElement.addElement("flow", "http://www.mulesoft.org/schema/mule/core");
            parentElement.addAttribute("name", flowName);
        }
        try
        {
            parentElement.add(convert(element));

            processGlobalReferences(element, callback, rootElement, schemaLocations);

            // For message sources to work, the flow should be valid, this means needs to have a MP
            if (embedInFlow)
            {
                parentElement.addElement("logger", "http://www.mulesoft.org/schema/mule/core");
            }

            setSchemaLocation(rootElement, schemaLocations);

            return document.asXML();
        }
        catch (Throwable t)
        {
            throw new MuleArtifactFactoryException("Error generating minimal XML configuration.", t);
        }

    }

    private void processGlobalReferences(org.w3c.dom.Element element, XmlConfigurationCallback callback, Element rootElement, Map<String, String> schemaLocations) throws ParserConfigurationException
    {
        processGlobalReferencesInAttributes(element, callback, rootElement, schemaLocations);

        processGlobalReferencesInChildElements(element, callback, rootElement, schemaLocations);
    }

    private void processGlobalReferencesInChildElements(org.w3c.dom.Element element, XmlConfigurationCallback callback, Element rootElement, Map<String, String> schemaLocations) throws ParserConfigurationException
    {
        if (element != null && element.getChildNodes() != null)
        {
            // Look for references in first level of child nodes
            for (int i = 0; i < element.getChildNodes().getLength(); i++)
            {
                processGlobalReferencesInAttributes(element.getChildNodes().item(i), callback, rootElement, schemaLocations);
            }
        }
    }

    private void processGlobalReferencesInAttributes(Node element, XmlConfigurationCallback callback, Element rootElement, Map<String, String> schemaLocations) throws ParserConfigurationException
    {
        if (element != null && element.getAttributes() != null)
        {
            for (int i = 0; i < element.getAttributes().getLength(); i++)
            {
                String attributeName = element.getAttributes().item(i).getLocalName();
                if (attributeName != null && ((attributeName.endsWith(REF_SUFFIX) || attributeName.equals(REF_ATTRIBUTE_NAME)) || (element.getNodeName().endsWith(REF_SUFFIX) && attributeName.equals(NAME_ATTRIBUTE_NAME))))
                {
                    org.w3c.dom.Element dependentElement = callback.getGlobalElement(element.getAttributes()
                                                                                             .item(i)
                                                                                             .getNodeValue());
                    addReferencedGlobalElement(callback, rootElement, dependentElement, schemaLocations);
                }
                else if (attributeName != null && attributeName.endsWith(REFS_SUFFIX))
                {
                    StringTokenizer refs = new StringTokenizer(element.getAttributes().item(i).getNodeValue(), REFS_TOKENS);

                    while(refs.hasMoreTokens())
                    {
                        String referenceName = refs.nextToken();
                        if (StringUtils.isNotBlank(referenceName))
                        {
                            org.w3c.dom.Element dependentElement = callback.getGlobalElement(referenceName);
                            addReferencedGlobalElement(callback, rootElement, dependentElement, schemaLocations);
                        }

                    }
                }

            }
        }

    }

    private void addReferencedGlobalElement(XmlConfigurationCallback callback, Element rootElement, org.w3c.dom.Element dependentElement, Map<String, String> schemaLocations) throws ParserConfigurationException
    {
        if (dependentElement != null)
        {
            if (isSpringBean(dependentElement))
            {
                wrapElementInSpringBeanContainer(rootElement, dependentElement);
            }
            else
            {
                rootElement.add(convert(dependentElement));
                addSchemaLocation(dependentElement, callback, schemaLocations);
            }
            processGlobalReferences(dependentElement, callback, rootElement, schemaLocations);
        }
    }

    private void wrapElementInSpringBeanContainer(Element rootElement, org.w3c.dom.Element dependentElement) throws ParserConfigurationException
    {
        String namespaceUri = dependentElement.getNamespaceURI();
        Namespace namespace = new Namespace(dependentElement.getPrefix(), namespaceUri);
        Element beans = rootElement.element(new QName(BEANS_ELEMENT, namespace));
        if (beans == null)
        {
            beans = rootElement.addElement(BEANS_ELEMENT, namespaceUri);
        }
        beans.add(convert(dependentElement));
    }

    private boolean isSpringBean(org.w3c.dom.Element dependentElement)
    {
        return "http://www.springframework.org/schema/beans".equals(dependentElement.getNamespaceURI());
    }

    protected MuleArtifact doGetArtifact(org.w3c.dom.Element element, final XmlConfigurationCallback callback, boolean embedInFlow)
            throws MuleArtifactFactoryException
    {
        String flowName = "flow-" + Integer.toString(element.hashCode());
        InputStream xmlConfig = IOUtils.toInputStream(getArtifactMuleConfig(flowName, element, callback, embedInFlow));

        MuleContext muleContext = null;
        SpringXmlConfigurationBuilder builder = null;
        Map<String, String> environmentProperties = callback.getEnvironmentProperties();
        Properties systemProperties = System.getProperties();
        Map<Object, Object> originalSystemProperties = new HashMap<Object, Object>(systemProperties);

        try
        {
            ConfigResource config = new ConfigResource("embedded-datasense.xml", xmlConfig);
            // This configuration overrides the default-mule-config one to replace beans that are not required
            ConfigResource defaultConfigOverride = new ConfigResource(getClass().getClassLoader().getResource("default-mule-config-override.xml").toURI().toURL());

            if (environmentProperties != null)
            {
                systemProperties.putAll(environmentProperties);
            }
            MuleContextFactory factory = new DefaultMuleContextFactory();

            builder = new SpringXmlConfigurationBuilder(new ConfigResource[] {config, defaultConfigOverride});
            muleContext = factory.createMuleContext(builder);
            muleContext.start();

            MuleArtifact artifact = null;
            if (embedInFlow)
            {
                Pipeline pipeline = muleContext.getRegistry().lookupObject(flowName);
                if (pipeline.getMessageSource() == null)
                {
                    if (pipeline.getMessageProcessors() != null && pipeline.getMessageProcessors().size() > 0)
                    {
                        artifact = new DefaultMuleArtifact(pipeline.getMessageProcessors().get(0));
                    }
                    else
                    {
                        throw new IllegalArgumentException("artifact is null");
                    }
                }
                else
                {
                    artifact = new DefaultMuleArtifact(pipeline.getMessageSource());
                }
            }
            else
            {
                artifact = new DefaultMuleArtifact(muleContext.getRegistry().lookupObject(element.getAttribute("name")));
            }

            builders.put(artifact, builder);
            contexts.put(artifact, muleContext);
            return artifact;
        }
        catch (ConfigurationException ce)
        {
            dispose(builder, muleContext);
            throw new MuleArtifactFactoryException("There seems to be a problem in your XML configuration. Please fix and retry.", ce);
        }
        catch (Throwable t)
        {
            dispose(builder, muleContext);
            throw new MuleArtifactFactoryException("Error starting minimal XML configuration.", t);
        }
        finally
        {
            systemProperties.clear();
            systemProperties.putAll(originalSystemProperties);
            IOUtils.closeQuietly(xmlConfig);
        }
    }

    protected void addSchemaLocation(org.w3c.dom.Element element, XmlConfigurationCallback callback,
                                     Map<String, String> schemaLocations)
    {
        String key = element.getNamespaceURI();
        if (key != null && !schemaLocations.containsKey(key))
        {
            schemaLocations.put(element.getNamespaceURI(), callback.getSchemaLocation(element.getNamespaceURI()));
        }
    }

    protected void setSchemaLocation(Element rootElement, Map<String, String> schemaLocations)
    {
        StringBuilder schemaLocation = new StringBuilder();

        for (String key : schemaLocations.keySet())
        {
            schemaLocation.append(key + " " + schemaLocations.get(key) + "\n");
        }

        rootElement.addAttribute(
                org.dom4j.QName.get("schemaLocation", "xsi", "http://www.w3.org/2001/XMLSchema-instance"),
                schemaLocation.toString().trim());
    }

    /**
     * Convert w3c element to dom4j element
     *
     * @throws ParserConfigurationException
     */
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

    @Override
    public void returnArtifact(MuleArtifact artifact)
    {
        SpringXmlConfigurationBuilder builder = builders.remove(artifact);
        MuleContext context = contexts.remove(artifact);
        dispose(builder, context);
    }

    protected void dispose(SpringXmlConfigurationBuilder builder, MuleContext context)
    {
        try
        {
            if (context != null)
            {
                context.dispose();
            }
        }
        finally
        {
            deleteLoggingThreads();
        }
    }

    private void deleteLoggingThreads()
    {
        String[] threadsToDelete = {"Mule.log.clogging.ref.handler", "Mule.log.slf4j.ref.handler"};

        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
        for (String threadToDelete : threadsToDelete)
        {
            for (Thread t : threadArray)
            {
                if (threadToDelete.equals(t.getName()))
                {
                    try
                    {
                        t.interrupt();
                    }
                    catch (SecurityException e)
                    {
                        // ignore
                    }
                }
            }
        }
    }
}
