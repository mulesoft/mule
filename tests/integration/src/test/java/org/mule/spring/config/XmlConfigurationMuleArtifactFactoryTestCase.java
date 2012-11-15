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

import java.io.IOException;
import java.util.ServiceLoader;

import junit.framework.Assert;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class XmlConfigurationMuleArtifactFactoryTestCase extends AbstractMuleTestCase
{

    @Test(expected = MuleArtifactFactoryException.class)
    public void testMissingAttribute() throws SAXException, IOException, MuleArtifactFactoryException
    {
        Document document = XMLUnit.buildControlDocument("<jdbc:connector name=\"jdbcConnector\" pollingFrequency=\"1000\" queryTimeout=\"3000\" xmlns:jdbc=\"http://www.mulesoft.org/schema/mule/jdbc\"/>");

        System.out.println((lookupArtifact().getArtifact(document.getDocumentElement(),
            getXmlConfigurationCallback(true))));
    }

    @Test(expected = MuleArtifactFactoryException.class)
    public void testMissingDependentElement() throws SAXException, IOException, MuleArtifactFactoryException
    {
        Document document = XMLUnit.buildControlDocument("<jdbc:connector name=\"jdbcConnector\" pollingFrequency=\"1000\" dataSource-ref=\"jdbcDataSource\" queryTimeout=\"3000\" xmlns:jdbc=\"http://www.mulesoft.org/schema/mule/jdbc\"/>");

        System.out.println((lookupArtifact().getArtifact(document.getDocumentElement(),
            getXmlConfigurationCallback(false))));
    }

    @Test
    public void testOK() throws SAXException, IOException, MuleArtifactFactoryException
    {
        Document document = XMLUnit.buildControlDocument("<jdbc:connector name=\"jdbcConnector\" pollingFrequency=\"1000\" dataSource-ref=\"jdbcDataSource\" queryTimeout=\"3000\" xmlns:jdbc=\"http://www.mulesoft.org/schema/mule/jdbc\"/>");

        MuleArtifact artifact = lookupArtifact().getArtifact(document.getDocumentElement(),
            getXmlConfigurationCallback(true));

        Assert.assertNotNull(artifact);
        Assert.assertTrue(artifact.hasCapability(Testable.class));
        Assert.assertTrue(artifact.getCapability(Testable.class) instanceof Testable);
    }
    
    @Test
    public void testMySqlOK() throws SAXException, IOException, MuleArtifactFactoryException
    {
    	String config = "<jdbc:connector name=\"jdbcConnector\" pollingFrequency=\"1000\" dataSource-ref=\"mysqlDatasource\" queryTimeout=\"3000\" xmlns:jdbc=\"http://www.mulesoft.org/schema/mule/jdbc\"/>";
        Document document = XMLUnit.buildControlDocument(config);

        MuleArtifact artifact = lookupArtifact().getArtifact(document.getDocumentElement(),
            getXmlConfigurationCallback(true));

        Assert.assertNotNull(artifact);
        Assert.assertTrue(artifact.hasCapability(Testable.class));
        Assert.assertTrue(artifact.getCapability(Testable.class) instanceof Testable);
//    	Testable t = artifact.getCapability(Testable.class);
//        Assert.assertEquals(TestResult.Status.SUCCESS, t.test().getStatus());
    }

    protected XmlConfigurationCallback getXmlConfigurationCallback(final boolean datasourceConfigured)
    {
        return new XmlConfigurationCallback()
        {

            @Override
            public String getSchemaLocation(String arg0)
            {
                if (arg0.equals("http://www.mulesoft.org/schema/mule/jdbc"))
                {
                    return "http://www.mulesoft.org/schema/mule/jdbc/current/mule-jdbc.xsd";
                }
                else
                {
                    return null;
                }
            }

            @Override
            public Element getGlobalElement(String arg0)
            {
                if (datasourceConfigured && arg0.equals("jdbcDataSource"))
                {
                    try
                    {
                        return XMLUnit.buildControlDocument(
                            "<jdbc:derby-data-source name=\"jdbcDataSource\" url=\"jdbc:derby:muleEmbeddedDB;create=true\"  xmlns:jdbc=\"http://www.mulesoft.org/schema/mule/jdbc\"/>")
                            .getDocumentElement();
                    }
                    catch (Exception e)
                    {
                        return null;
                    }
                }
                else if (datasourceConfigured && arg0.equals("mysqlDatasource"))
                {
                    try
                    {
                        return XMLUnit.buildControlDocument(
                        	"<jdbc:mysql-data-source name=\"mysqlDatasource\" user=\"myUser\" password=\"secret\" host=\"localhost\" database=\"test\"  port=\"3306\" xmlns:jdbc=\"http://www.mulesoft.org/schema/mule/jdbc\"/>")
                            .getDocumentElement();
                    }
                    catch (Exception e)
                    {
                        return null;
                    }
                }
                else
                {
                    return null;
                }
            }
        };
    }

    protected static XmlConfigurationMuleArtifactFactory lookupArtifact()
    {
        return ServiceLoader.load(XmlConfigurationMuleArtifactFactory.class).iterator().next();
    }

}
