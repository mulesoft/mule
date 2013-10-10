/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

import static org.junit.Assert.assertEquals;

import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.ClassUtils;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.Test;

public class SchemaDefaultsTestCase extends AbstractMuleContextTestCase
{
    private static String MULE_CORE_SCHEMA_FILE = "META-INF/mule.xsd";
    private Document schema;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        SAXReader reader = new SAXReader();
        schema = reader.read(ClassUtils.getResource(MULE_CORE_SCHEMA_FILE, this.getClass()).openStream());
    }
    
    @Override
    protected boolean isGracefulShutdown()
    {
        return true;
    }

    @Test
    public void testConfigurationDefaults()
    {
        Element configurationType = (Element) schema.selectSingleNode("/xsd:schema/xsd:complexType[@name='configurationType']");

        assertEquals(muleContext.getConfiguration().getDefaultResponseTimeout(),
            configurationType.numberValueOf("xsd:complexContent/xsd:extension/xsd:attribute[@name='defaultResponseTimeout']/@default")
                .intValue());
        assertEquals(muleContext.getConfiguration().getDefaultTransactionTimeout(),
            configurationType.numberValueOf("xsd:complexContent/xsd:extension/xsd:attribute[@name='defaultTransactionTimeout']/@default")
                .intValue());
        assertEquals(muleContext.getConfiguration().getShutdownTimeout(),
            configurationType.numberValueOf("xsd:complexContent/xsd:extension/xsd:attribute[@name='shutdownTimeout']/@default")
                .intValue());
        assertEquals(muleContext.getConfiguration().useExtendedTransformations(), Boolean.parseBoolean(
                     configurationType.valueOf("xsd:complexContent/xsd:extension/xsd:attribute[@name='useExtendedTransformations']/@default")));
        assertEquals(muleContext.getConfiguration().isFlowEndingWithOneWayEndpointReturnsNull(), Boolean.parseBoolean(
                configurationType.valueOf("xsd:complexContent/xsd:extension/xsd:attribute[@name='flowEndingWithOneWayEndpointReturnsNull']/@default")));
        assertEquals(
            muleContext.getConfiguration().isEnricherPropagatesSessionVariableChanges(),
            Boolean.parseBoolean(configurationType.valueOf("xsd:complexContent/xsd:extension/xsd:attribute[@name='enricherPropagatesSessionVariableChanges']/@default")));        
    }
}
