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

import org.mule.common.MuleArtifactFactoryException;
import org.mule.common.config.XmlConfigurationCallback;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class DatabaseMuleArtifactTestCase extends XmlConfigurationMuleArtifactFactoryTestCase
{
    @Test(expected = MuleArtifactFactoryException.class)
    public void testMissingAttribute() throws SAXException, IOException, MuleArtifactFactoryException
    {
        Document document = XMLUnit.buildControlDocument("<jdbc:connector name=\"jdbcConnector\" pollingFrequency=\"1000\" queryTimeout=\"3000\" xmlns:jdbc=\"http://www.mulesoft.org/schema/mule/jdbc\"/>");
        XmlConfigurationCallback callback = new DatabaseConfigurationCallback();

        System.out.println((lookupArtifact().getArtifact(document.getDocumentElement(), callback)));
    }

    @Test(expected = MuleArtifactFactoryException.class)
    public void testMissingDependentElement() throws SAXException, IOException, MuleArtifactFactoryException
    {
        Document document = XMLUnit.buildControlDocument("<jdbc:connector name=\"jdbcConnector\" pollingFrequency=\"1000\" dataSource-ref=\"unknownJdbcDataSource\" queryTimeout=\"3000\" xmlns:jdbc=\"http://www.mulesoft.org/schema/mule/jdbc\"/>");
        XmlConfigurationCallback callback = new DatabaseConfigurationCallback();

        System.out.println((lookupArtifact().getArtifact(document.getDocumentElement(), callback)));
    }


    @Override
    public int getTestTimeoutSecs() {
        return 99999;
    }

    protected static class DatabaseConfigurationCallback extends MapXmlConfigurationCallback
    {
        private static Map<String,String> SCHEMA_MAP = new HashMap<String, String>();
        static
        {
            SCHEMA_MAP.put("http://www.springframework.org/schema/jdbc", "http://www.springframework.org/schema/jdbc/spring-jdbc.xsd");
            SCHEMA_MAP.put("http://www.mulesoft.org/schema/mule/jdbc", "http://www.mulesoft.org/schema/mule/jdbc/current/mule-jdbc.xsd");
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

    @Test
    public void testDerby() throws SAXException, IOException, MuleArtifactFactoryException
    {
        String config = "<jdbc:connector name=\"jdbcConnector\" pollingFrequency=\"1000\" dataSource-ref=\"jdbcDataSource\" queryTimeout=\"3000\" xmlns:jdbc=\"http://www.mulesoft.org/schema/mule/jdbc\"/>";
        Document document = XMLUnit.buildControlDocument(config);
        String refDef =
                //              "<jdbc:derby-data-source name=\"jdbcDataSource\" url=\"jdbc:derby:muleEmbeddedDB;create=true\"  xmlns:jdbc=\"http://www.mulesoft.org/schema/mule/jdbc\"/>"

                //          	"<spring:bean xmlns:spring=\"http://www.springframework.org/schema/beans\" class=\"org.apache.derby.jdbc.EmbeddedXADataSource\" id=\"jdbcDataSource\" name=\"Bean\">"
                //          	+ "<spring:property name=\"databaseName\" value=\"muleEmbeddedDB\"/>"
                //				+ "</spring:bean>"

                //          	"<spring:bean xmlns:spring=\"http://www.springframework.org/schema/beans\" class=\"org.enhydra.jdbc.standard.StandardXADataSource\" destroy-method=\"shutdown\" id=\"jdbcDataSource\" name=\"Bean\">"
                //				+ "<spring:property name=\"driverName\" value=\"org.apache.derby.jdbc.EmbeddedDriver\"/>"
                //				+ "<spring:property name=\"url\" value=\"jdbc:derby:muleEmbeddedDB;create=true\"/>"
                //				+ "</spring:bean>"

                "<spring:bean xmlns:spring=\"http://www.springframework.org/schema/beans\" class=\"org.apache.commons.dbcp.BasicDataSource\" destroy-method=\"close\" id=\"jdbcDataSource\" name=\"Bean\">"
                + "<spring:property name=\"driverClassName\" value=\"org.apache.derby.jdbc.EmbeddedDriver\"/>"
                + "<spring:property name=\"url\" value=\"jdbc:derby:muleEmbeddedDB;create=true\"/>"
                + "</spring:bean>"

                //              	"<spring:bean xmlns:spring=\"http://www.springframework.org/schema/beans\" class=\"com.mchange.v2.c3p0.ComboPooledDataSource\" destroy-method=\"close\" id=\"jdbcDataSource\" name=\"Bean\">"
                //					+ "<spring:property name=\"driverClass\" value=\"org.apache.derby.jdbc.EmbeddedDriver\"/>"
                //					+ "<spring:property name=\"jdbcUrl\" value=\"jdbc:derby:muleEmbeddedDB;create=true\"/>"
                //					+ "</spring:bean>"
                ;
        XmlConfigurationCallback callback = new DatabaseConfigurationCallback(Collections.singletonMap("jdbcDataSource", refDef));

        doTest(document, callback);
        doTest(document, callback);
        doTest(document, callback);
    }

    // ignored mysql test because mysql.mysql-connector-java/mysql-connector-java-x.jar is not on build path by default
    // and now that the artifact is started at creation, the test cannot work without the jar.
    @Ignore
    @Test
    public void testMySql() throws SAXException, IOException, MuleArtifactFactoryException
    {
        String config = "<jdbc:connector name=\"jdbcConnector\" pollingFrequency=\"1000\" dataSource-ref=\"mysqlDatasource\" queryTimeout=\"3000\" xmlns:jdbc=\"http://www.mulesoft.org/schema/mule/jdbc\"/>";
        Document document = XMLUnit.buildControlDocument(config);
        String refDef =
                //    	"<jdbc:mysql-data-source name=\"mysqlDatasource\" user=\"myUser\" password=\"secret\" host=\"localhost\" database=\"test\"  port=\"3306\" xmlns:jdbc=\"http://www.mulesoft.org/schema/mule/jdbc\"/>"

                "<spring:bean xmlns:spring=\"http://www.springframework.org/schema/beans\" class=\"org.apache.commons.dbcp.BasicDataSource\" destroy-method=\"close\" id=\"mysqlDatasource\" name=\"Bean\">"
                + "<spring:property name=\"driverClassName\" value=\"com.mysql.jdbc.Driver\"/>"
                + "<spring:property name=\"url\" value=\"jdbc:mysql://localhost/test\"/>"
                + "<spring:property name=\"username\" value=\"myUser\"/>"
                + "<spring:property name=\"password\" value=\"secret\"/>"
                + "</spring:bean>"

                //        	"<spring:bean xmlns:spring=\"http://www.springframework.org/schema/beans\" class=\"org.enhydra.jdbc.standard.StandardXADataSource\" destroy-method=\"shutdown\" id=\"mysqlDatasource\" name=\"Bean\">"
                //			+ "<spring:property name=\"driverName\" value=\"com.mysql.jdbc.Driver\"/>"
                //			+ "<spring:property name=\"url\" value=\"jdbc:mysql://localhost/test\"/>"
                //			+ "<spring:property name=\"user\" value=\"myUser\"/>"
                //			+ "<spring:property name=\"password\" value=\"secret\"/>"
                //			+ "</spring:bean>"
                ;
        XmlConfigurationCallback callback = new DatabaseConfigurationCallback(Collections.singletonMap("mysqlDatasource", refDef));

        doTest(document, callback);
        doTest(document, callback);
        doTest(document, callback);
    }

    @Test
    public void testH2() throws SAXException, IOException, MuleArtifactFactoryException
    {
        String config = "<jdbc:connector name=\"jdbcConnector\" pollingFrequency=\"1000\" dataSource-ref=\"h2Datasource\" queryTimeout=\"3000\" xmlns:jdbc=\"http://www.mulesoft.org/schema/mule/jdbc\"/>";
        Document document = XMLUnit.buildControlDocument(config);

        String refDef =
                "<spring:bean xmlns:spring=\"http://www.springframework.org/schema/beans\" class=\"org.h2.jdbcx.JdbcConnectionPool\" destroy-method=\"dispose\" id=\"h2Datasource\">"
                + "  <spring:constructor-arg>"
                + "    <spring:bean class=\"org.h2.jdbcx.JdbcDataSource\">"
                + "      <spring:property name=\"URL\" value=\"jdbc:h2:dbname\"/>"
                + "      <spring:property name=\"user\" value=\"user\"/>"
                + "      <spring:property name=\"password\" value=\"password\"/>"
                + "    </spring:bean>"
                + "  </spring:constructor-arg>"
                + "</spring:bean>"
                ;
        XmlConfigurationCallback callback = new DatabaseConfigurationCallback(Collections.singletonMap("h2Datasource", refDef));
        doTest(document, callback);
        doTest(document, callback);
        doTest(document, callback);

        refDef =
                "<spring:bean xmlns:spring=\"http://www.springframework.org/schema/beans\" class=\"org.enhydra.jdbc.standard.StandardXADataSource\" destroy-method=\"shutdown\" id=\"h2Datasource\">"
                + "<spring:property name=\"driverName\" value=\"org.h2.Driver\"/>"
                + "<spring:property name=\"url\" value=\"jdbc:h2:dbname\"/>"
                + "<spring:property name=\"user\" value=\"user\"/>"
                + "<spring:property name=\"password\" value=\"password\"/>"
                + "</spring:bean>"
        ;
        callback = new DatabaseConfigurationCallback(Collections.singletonMap("h2Datasource", refDef));
        doTest(document, callback);
        doTest(document, callback);
        doTest(document, callback);

        refDef =
                "<spring:bean xmlns:spring=\"http://www.springframework.org/schema/beans\" class=\"org.apache.commons.dbcp.BasicDataSource\" destroy-method=\"close\" id=\"h2Datasource\">"
                + "<spring:property name=\"driverClassName\" value=\"org.h2.Driver\"/>"
                + "<spring:property name=\"url\" value=\"jdbc:h2:dbname\"/>"
                + "<spring:property name=\"username\" value=\"user\"/>"
                + "<spring:property name=\"password\" value=\"password\"/>"
                + "</spring:bean>"
        ;
        callback = new DatabaseConfigurationCallback(Collections.singletonMap("h2Datasource", refDef));
        doTest(document, callback);
        doTest(document, callback);
        doTest(document, callback);
    }

    @Test
    public void testHsql() throws SAXException, IOException, MuleArtifactFactoryException
    {
        String config = "<jdbc:connector name=\"jdbcConnector\" pollingFrequency=\"1000\" dataSource-ref=\"hsqlDatasource\" queryTimeout=\"3000\" xmlns:jdbc=\"http://www.mulesoft.org/schema/mule/jdbc\"/>";
        Document document = XMLUnit.buildControlDocument(config);

        //String refDef =
        //        "<spring:bean xmlns:spring=\"http://www.springframework.org/schema/beans\" class=\"org.h2.jdbcx.JdbcConnectionPool\" destroy-method=\"dispose\" id=\"hsqlDatasource\">"
        //        + "<springjdbc:embedded-database xmlns:springjdbc=\"http://www.springframework.org/schema/jdbc\" id=\"embeddedDatasource\" type=\"HSQL\"/>"
        //        + "</spring:bean>"
        //        ;
        //XmlConfigurationCallback callback = new DatabaseConfigurationCallback(Collections.singletonMap("hsqlDatasource", refDef));
        //doTest(document, callback);
        //doTest(document, callback);
        //doTest(document, callback);

        String refDef =
                "<spring:bean xmlns:spring=\"http://www.springframework.org/schema/beans\" class=\"org.apache.commons.dbcp.BasicDataSource\" destroy-method=\"close\" id=\"hsqlDatasource\">"
                + "<spring:property name=\"driverClassName\" value=\"org.hsqldb.jdbcDriver\"/>"
                + "<spring:property name=\"url\" value=\"jdbc:hsqldb:mem:spring-playground\"/>"
                + "<spring:property name=\"username\" value=\"sa\"/>"
                + "<spring:property name=\"password\" value=\"\"/>"
                + "</spring:bean>"
        ;
        XmlConfigurationCallback callback = new DatabaseConfigurationCallback(Collections.singletonMap("hsqlDatasource", refDef));
        doTest(document, callback);
        doTest(document, callback);
        doTest(document, callback);
    }

}
