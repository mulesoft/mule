/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import org.mule.common.MuleArtifact;
import org.mule.common.MuleArtifactFactoryException;
import org.mule.common.TestResult;
import org.mule.common.Testable;
import org.mule.common.config.XmlConfigurationCallback;
import org.mule.common.config.XmlConfigurationMuleArtifactFactory;
import org.mule.common.metadata.OperationMetaDataEnabled;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Map;
import java.util.ServiceLoader;

import org.custommonkey.xmlunit.XMLUnit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class XmlConfigurationMuleArtifactFactoryTestCase extends AbstractMuleTestCase
{

    protected void doTestMessageProcessorArtifactRetrieval(Document document, XmlConfigurationCallback callback) throws MuleArtifactFactoryException
    {
        XmlConfigurationMuleArtifactFactory factory = lookupArtifact();
        MuleArtifact artifact = null;

        try
        {
            artifact = factory.getArtifactForMessageProcessor(document.getDocumentElement(), callback);

            assertNotNull(artifact);
        }
        finally
        {
            if (artifact != null)
            {
                factory.returnArtifact(artifact);
            }
        }
    }

    protected void doTestMessageProcessorCapabilities(Document document, XmlConfigurationCallback callback) throws MuleArtifactFactoryException
    {
        doTestMessageProcessor(document, callback, null);
    }

    protected void doTestMessageProcessor(Document document, XmlConfigurationCallback callback) throws MuleArtifactFactoryException
    {
        doTestMessageProcessor(document, callback, TestResult.Status.SUCCESS);
    }

    protected void doTest(Document document, XmlConfigurationCallback callback) throws MuleArtifactFactoryException
    {
        doTest(document, callback, TestResult.Status.SUCCESS);
    }

    private void doTestMessageProcessor(Document document, XmlConfigurationCallback callback, TestResult.Status expectedResult)
            throws MuleArtifactFactoryException
    {
        XmlConfigurationMuleArtifactFactory factory = lookupArtifact();
        MuleArtifact artifact = null;

        try
        {
            artifact = factory.getArtifactForMessageProcessor(document.getDocumentElement(), callback);

            assertNotNull(artifact);
            assertTrue(artifact.hasCapability(OperationMetaDataEnabled.class));
            assertTrue(artifact.getCapability(OperationMetaDataEnabled.class) instanceof OperationMetaDataEnabled);
            if (expectedResult != null)
            {
                OperationMetaDataEnabled artifactCapability = artifact.getCapability(OperationMetaDataEnabled.class);
                assertNull(artifactCapability.getInputMetaData());
                assertEquals(expectedResult, artifactCapability.getOutputMetaData(null).getStatus());
            }
        }
        finally
        {
            if (artifact != null)
            {
                factory.returnArtifact(artifact);
            }
        }

    }

    private void doTest(Document document, XmlConfigurationCallback callback, TestResult.Status expectedResult)
            throws MuleArtifactFactoryException
    {
        XmlConfigurationMuleArtifactFactory factory = lookupArtifact();
        MuleArtifact artifact = null;

        try
        {
            artifact = factory.getArtifact(document.getDocumentElement(), callback);
            assertNotNull(artifact);
            assertTrue(artifact.hasCapability(Testable.class));
            assertTrue(artifact.getCapability(Testable.class) instanceof Testable);
            if (expectedResult != null)
            {
                Testable artifactCapability = artifact.getCapability(Testable.class);
                assertEquals(expectedResult, artifactCapability.test().getStatus());
            }
        }
        finally
        {
            if (artifact != null)
            {
                factory.returnArtifact(artifact);
            }
        }

    }

    protected static XmlConfigurationMuleArtifactFactory lookupArtifact()
    {
        return ServiceLoader.load(XmlConfigurationMuleArtifactFactory.class).iterator().next();
    }

    protected static class MapXmlConfigurationCallback implements XmlConfigurationCallback
    {
        private Map<String, String> refNameToXml;
        private Map<String, String> namespaceUriToSchemaLocation;

        public MapXmlConfigurationCallback()
        {
            this(null, null);
        }

        public MapXmlConfigurationCallback(Map<String, String> refNameToXml, Map<String, String> namespaceUriToSchemaLocation)
        {
            this.refNameToXml = refNameToXml;
            this.namespaceUriToSchemaLocation = namespaceUriToSchemaLocation;
        }

        @Override
        public Element getGlobalElement(String globalElementName) {
            if (refNameToXml != null)
            {
                String xml = refNameToXml.get(globalElementName);
                if (xml != null)
                {
                    try
                    {
                        return XMLUnit.buildControlDocument(xml).getDocumentElement();
                    }
                    catch (Exception e)
                    {
                        return null;
                    }
                }
            }
            return null;
        }

        @Override
        public String getSchemaLocation(String namespaceUri)
        {
            if (namespaceUriToSchemaLocation != null)
            {
                return namespaceUriToSchemaLocation.get(namespaceUri);
            }
            else
            {
                return null;
            }
        }

        public Element[] getPropertyPlaceholders()
        {
            return new Element[0];
        }

        public Map<String, String> getEnvironmentProperties()
        {
            return null;
        }
    }
}




