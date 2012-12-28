/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.spring.config;

import org.mule.common.MuleArtifact;
import org.mule.common.MuleArtifactFactoryException;
import org.mule.common.TestResult;
import org.mule.common.Testable;
import org.mule.common.config.XmlConfigurationCallback;
import org.mule.common.config.XmlConfigurationMuleArtifactFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Map;
import java.util.ServiceLoader;

import junit.framework.Assert;
import org.custommonkey.xmlunit.XMLUnit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class XmlConfigurationMuleArtifactFactoryTestCase extends AbstractMuleTestCase
{

    protected void doTest(Document document, XmlConfigurationCallback callback) throws MuleArtifactFactoryException
    {
        doTest(document, callback, TestResult.Status.SUCCESS);
    }

    protected void doTest(Document document, XmlConfigurationCallback callback, TestResult.Status expectedResult)
            throws MuleArtifactFactoryException
    {
        XmlConfigurationMuleArtifactFactory factory = lookupArtifact();
        MuleArtifact artifact = factory.getArtifact(document.getDocumentElement(), callback);

        Assert.assertNotNull(artifact);
        Assert.assertTrue(artifact.hasCapability(Testable.class));
        Assert.assertTrue(artifact.getCapability(Testable.class) instanceof Testable);
        Testable t = artifact.getCapability(Testable.class);
        Assert.assertEquals(expectedResult, t.test().getStatus());
        factory.returnArtifact(artifact);
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
    }
}




