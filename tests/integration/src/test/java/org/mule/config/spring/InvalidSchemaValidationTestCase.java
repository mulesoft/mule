/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.commons.collections.map.HashedMap;
import org.junit.After;
import org.junit.Test;
import org.xml.sax.SAXException;

public class InvalidSchemaValidationTestCase extends AbstractSchemaValidationTestCase
{
    @After
    public void doCleanUp()
    {
        schemas = new HashedMap();
    }

    @Test(expected = SAXException.class)
    public void testTransformerReference() throws SAXException, IOException
    {
        addSchema("http://www.mulesoft.org/schema/mule/vm","META-INF/mule-vm.xsd");
        doTest("org/mule/config/spring/schema-validation-transformer-ref-test.xml");
    }

    @Test(expected = SAXException.class)
    public void testFilterReference() throws SAXException, IOException
    {
        addSchema("http://www.mulesoft.org/schema/mule/vm","META-INF/mule-vm.xsd");
        doTest("org/mule/config/spring/schema-validation-filter-ref-test.xml");
    }

    @Test
    public void testTransactedConnectors() throws SAXException, IOException
    {
        addSchema("http://www.mulesoft.org/schema/mule/vm","META-INF/mule-vm.xsd");
        addSchema("http://www.mulesoft.org/schema/mule/jdbc","META-INF/mule-jdbc.xsd");
        addSchema("http://www.springframework.org/schema/beans", "http://www.springframework.org/schema/beans/spring-beans-3.1.xsd");
        doTest("org/mule/config/spring/schema-validation-transacted-connectors-test.xml");
    }

    @Test(expected = SAXException.class)
    public void testNotTransactedFileConnector() throws SAXException, IOException
    {
        addSchema("http://www.mulesoft.org/schema/mule/file", "META-INF/mule-file.xsd");
        doTest("org/mule/config/spring/schema-validation-not-transacted-file-connector-test.xml");
    }

    @Test(expected = SAXException.class)
    public void testNotTransactedFtpConnector() throws SAXException, IOException
    {
        addSchema("http://www.mulesoft.org/schema/mule/ftp", "META-INF/mule-ftp.xsd");
        doTest("org/mule/config/spring/schema-validation-not-transacted-ftp-connector-test.xml");
    }

    @Test
    public void testJdbcInvalidPollingFrequencyInOutboundEndpoint() throws SAXException, IOException
    {
        addSchema("http://www.mulesoft.org/schema/mule/jdbc","META-INF/mule-jdbc.xsd");
        addSchema("http://www.mulesoft.org/schema/mule/test", "http://www.mulesoft.org/schema/mule/test/3.2/mule-test.xsd");
        try
        {
            doTest("org/mule/config/spring/schema-validation-jdbc-invalid-polling-frequency.xml");
        }
        catch(SAXException e)
        {
            // Check that the pollingFrequency exception is because of the outbound endpoint and not the inbound
            assertTrue(e.getMessage() != null && e.getMessage().contains("jdbc:outbound-endpoint"));
        }
    }

    @Test
    public void testExpressionPattern() throws SAXException, IOException
    {
        doTest("org/mule/config/spring/schema-validation-expression-pattern-test.xml");
    }

    @Test
    public void testRouterWithFilter() throws SAXException, IOException
    {
        addSchema("http://www.mulesoft.org/schema/mule/vm","META-INF/mule-vm.xsd");
        doTest("org/mule/config/spring/schema-validation-router-filter-test.xml");
    }
}
