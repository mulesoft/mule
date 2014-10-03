/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.integration.vendor.oracle;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.module.db.integration.DbTestUtil.selectData;
import static org.mule.module.db.integration.TestRecordUtil.assertRecords;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.Alien;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.Record;
import org.mule.module.db.internal.domain.type.oracle.OracleXmlType;
import org.mule.util.IOUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class OracleInsertXmlTypeTestCase extends AbstractOracleXmlTypeTestCase
{

    public OracleInsertXmlTypeTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getOracleResource();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/vendor/oracle/oracle-insert-xml-type-config.xml"};
    }

    @Test
    public void insertXmlTypeFromXmlType() throws Exception
    {
        assertAlienWasInserted(
                doTest(new XmlContentBuilder()
                {
                    @Override
                    public Object build(Connection connection) throws Exception
                    {
                        return OracleXmlType.createXmlType(connection, Alien.ET.getXml());
                    }
                }));
    }

    @Test
    public void insertLargeXmlTypeFromInputStream() throws Exception
    {
        assertAlienWasInserted(
                doTest(new XmlContentBuilder()
                {
                    @Override
                    public Object build(Connection connection) throws Exception
                    {
                        return IOUtils.getResourceAsStream("integration/vendor/oracle/oracle-insert-xml-type-large-sample.xml", this.getClass());
                    }
                }));
    }

    @Test
    public void insertXmlTypeFromString() throws Exception
    {
        assertAlienWasInserted(
                doTest(new XmlContentBuilder()
                {
                    @Override
                    public Object build(Connection connection) throws Exception
                    {
                        return Alien.ET.getXml();
                    }
                }));
    }

    @Test
    public void insertXmlTypeFromWrongType() throws Exception
    {
        assertNoAliens(
                doTest(new XmlContentBuilder()
                {
                    @Override
                    public Object build(Connection connection) throws Exception
                    {
                        return new Integer(1);
                    }
                }));
    }

    private void assertNoAliens(MuleMessage response) throws SQLException
    {
        assertThat(response.getExceptionPayload(), is(notNullValue()));

        List<Map<String, String>> result = selectData("SELECT name FROM Alien", getDefaultDataSource());
        assertRecords(result);
    }

    private void assertAlienWasInserted(MuleMessage response) throws SQLException
    {
        assertThat(response.getExceptionPayload(), is(nullValue()));

        assertThat((Integer) response.getPayload(), is(equalTo(1)));

        List<Map<String, String>> result = selectData("SELECT name FROM Alien", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", Alien.ET.getName())));
    }

    private interface XmlContentBuilder
    {
         Object build(Connection connection) throws Exception;
    }

    private MuleMessage doTest(XmlContentBuilder builder) throws Exception
    {
        DataSource defaultDataSource = getDefaultDataSource();
        Connection connection = defaultDataSource.getConnection();

        try
        {
            testDatabase.executeUpdate(connection, "DELETE FROM ALIEN");

            LocalMuleClient client = muleContext.getClient();

            Map<String, Object> messageProperties = new HashMap<String, Object>();
            messageProperties.put("name", Alien.ET.getName());

            return client.send("vm://insertXmlType", builder.build(connection), messageProperties);
        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }
    }
}
