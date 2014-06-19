/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.spring.config;

import org.mule.common.MuleArtifactFactoryException;
import org.mule.common.config.XmlConfigurationCallback;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class NewDatabaseMuleArtifactTestCase extends XmlConfigurationMuleArtifactFactoryTestCase
{

    @Test(expected = MuleArtifactFactoryException.class)
    public void detectsMissingAttribute() throws SAXException, IOException, MuleArtifactFactoryException
    {
        Document document = XMLUnit.buildControlDocument("<db:select xmlns:db=\"http://www.mulesoft.org/schema/mule/db\"><db:template-query-ref name=\"template\"/></db:select>");
        XmlConfigurationCallback callback = new DatabaseConfigurationCallback();

        lookupArtifact().getArtifactForMessageProcessor(document.getDocumentElement(), callback);
    }

    @Test(expected = MuleArtifactFactoryException.class)
    public void detectsMissingDependentElement() throws SAXException, IOException, MuleArtifactFactoryException
    {
        Document document = XMLUnit.buildControlDocument("<db:select config-ref=\"mysql-config\" xmlns:db=\"http://www.mulesoft.org/schema/mule/db\"><db:template-query-ref name=\"template\"/></db:select>");
        XmlConfigurationCallback callback = new DatabaseConfigurationCallback();

        lookupArtifact().getArtifactForMessageProcessor(document.getDocumentElement(), callback);
    }

    @Test
    public void validatesDbConnectorGenericMySqlTemplateQueryRefResolution() throws SAXException, IOException, MuleArtifactFactoryException
    {
        String config = "<db:select config-ref=\"mysql-config\" xmlns:db=\"http://www.mulesoft.org/schema/mule/db\"><db:template-query-ref name=\"template\"/></db:select>";
        Document document = XMLUnit.buildControlDocument(config);

        String configRef = "<db:generic-config name=\"mysql-config\" url=\"jdbc:mysql://localhost/test?user=myUser&amp;password=secret\" driverClassName=\"com.mysql.jdbc.Driver\" xmlns:db=\"http://www.mulesoft.org/schema/mule/db\" />";
        String templateRef = "<db:template-query name=\"template\" xmlns:db=\"http://www.mulesoft.org/schema/mule/db\"><db:parameterized-query><![CDATA[SELECT * FROM Users]]></db:parameterized-query><db:in-param name=\"myParameter1\" defaultValue=\"lala\"/><db:in-param name=\"myParameter2\" defaultValue=\"#[payload.parameter2]\"/></db:template-query>";

        Map<String, String> callbackData = new HashMap<String, String>();
        callbackData.put("mysql-config", configRef);
        callbackData.put("template", templateRef);

        XmlConfigurationCallback callback = new DatabaseConfigurationCallback(callbackData);
        doTestMessageProcessorCapabilities(document, callback);
    }

    @Test(expected = MuleArtifactFactoryException.class)
    public void detectsDbConnectorGenericMySqlMissingTemplateQueryRef() throws SAXException, IOException, MuleArtifactFactoryException
    {
        String config = "<db:select config-ref=\"mysql-config\" xmlns:db=\"http://www.mulesoft.org/schema/mule/db\"><db:template-query-ref name=\"template1\"/></db:select>";
        Document document = XMLUnit.buildControlDocument(config);

        String configRef = "<db:generic-config name=\"mysql-config\" url=\"jdbc:mysql://localhost/test?user=myUser&amp;password=secret\" driverClassName=\"com.mysql.jdbc.Driver\" xmlns:db=\"http://www.mulesoft.org/schema/mule/db\" />";
        String templateRef1 = "<db:template-query name=\"template1\" xmlns:db=\"http://www.mulesoft.org/schema/mule/db\"><db:template-query-ref name=\"template2\"/></db:template-query>";

        Map<String, String> callbackData = new HashMap<String, String>();
        callbackData.put("mysql-config", configRef);
        callbackData.put("template1", templateRef1);

        XmlConfigurationCallback callback = new DatabaseConfigurationCallback(callbackData);
        doTestMessageProcessorCapabilities(document, callback);
    }

    @Test
    public void validatesDbConnectorGenericMySqlOverriddenTemplateResolution() throws SAXException, IOException, MuleArtifactFactoryException
    {
        String config = "<db:select config-ref=\"mysql-config\" xmlns:db=\"http://www.mulesoft.org/schema/mule/db\"><db:template-query-ref name=\"template1\"/></db:select>";
        Document document = XMLUnit.buildControlDocument(config);

        String configRef = "<db:generic-config name=\"mysql-config\" url=\"jdbc:mysql://localhost/test?user=myUser&amp;password=secret\" driverClassName=\"com.mysql.jdbc.Driver\" xmlns:db=\"http://www.mulesoft.org/schema/mule/db\" />";
        String templateRef1 = "<db:template-query name=\"template1\" xmlns:db=\"http://www.mulesoft.org/schema/mule/db\"><db:template-query-ref name=\"template2\"/><db:in-param name=\"myParameter1\" defaultValue=\"jeje\"/></db:template-query>";
        String templateRef2 = "<db:template-query name=\"template2\" xmlns:db=\"http://www.mulesoft.org/schema/mule/db\"><db:parameterized-query><![CDATA[SELECT username, password FROM Users]]></db:parameterized-query><db:in-param name=\"myParameter1\" defaultValue=\"jeje\"/><db:in-param name=\"myParameter2\" defaultValue=\"#[payload.parameter]\"/></db:template-query>";

        Map<String, String> callbackData = new HashMap<String, String>();
        callbackData.put("mysql-config", configRef);
        callbackData.put("template1", templateRef1);
        callbackData.put("template2", templateRef2);

        XmlConfigurationCallback callback = new DatabaseConfigurationCallback(callbackData);
        doTestMessageProcessorCapabilities(document, callback);
    }

    @Test
    public void validatesDbDerbyConnectorTemplateQueryRefResolution() throws SAXException, IOException, MuleArtifactFactoryException
    {
        String config = "<db:select config-ref=\"derby-config\" xmlns:db=\"http://www.mulesoft.org/schema/mule/db\"><db:template-query-ref name=\"template\"/></db:select>";
        Document document = XMLUnit.buildControlDocument(config);

        String configRef = "<db:derby-config name=\"derby-config\" url=\"jdbc:derby:muleEmbeddedDB;create=true\" xmlns:db=\"http://www.mulesoft.org/schema/mule/db\" driverClassName=\"org.apache.derby.jdbc.EmbeddedDriver\"/>";
        String templateRef = "<db:template-query name=\"template\" xmlns:db=\"http://www.mulesoft.org/schema/mule/db\"><db:parameterized-query><![CDATA[SELECT CURRENT_TIMESTAMP FROM SYSIBM.SYSDUMMY1]]></db:parameterized-query></db:template-query>";

        Map<String, String> callbackData = new HashMap<String, String>();
        callbackData.put("derby-config", configRef);
        callbackData.put("template", templateRef);

        XmlConfigurationCallback callback = new DatabaseConfigurationCallback(callbackData);
        doTestMessageProcessor(document, callback);
    }

    protected static class DatabaseConfigurationCallback extends MapXmlConfigurationCallback
    {

        private static Map<String, String> SCHEMA_MAP = new HashMap<String, String>();

        static
        {
            SCHEMA_MAP.put("http://www.springframework.org/schema/jdbc", "http://www.springframework.org/schema/jdbc/spring-jdbc.xsd");
            SCHEMA_MAP.put("http://www.mulesoft.org/schema/mule/db", "http://www.mulesoft.org/schema/mule/db/current/mule-db.xsd");
        }

        public DatabaseConfigurationCallback()
        {
            this(null);
        }

        public DatabaseConfigurationCallback(Map<String, String> refNameToXml)
        {
            super(refNameToXml, SCHEMA_MAP);
        }
    }
}
