/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.integration.vendor.oracle;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
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
import org.mule.module.db.internal.config.domain.database.XmlTypeUtils;
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
        new TestExecutor()
        {
            @Override
            protected Object createXmlContent(Connection connection) throws Exception
            {
                return XmlTypeUtils.createXmlType(connection, Alien.ET.getXml());
            }
        }.execute();
    }

    @Test
    public void insertLargeXmlTypeFromInputStream() throws Exception
    {
        new TestExecutor()
        {
            @Override
            protected Object createXmlContent(Connection connection) throws Exception
            {
                return IOUtils.getResourceAsStream("integration/vendor/oracle/large-sample.xml", this.getClass());
            }
        }.execute();
    }

    @Test
    public void insertXmlTypeFromString() throws Exception
    {
        new TestExecutor()
        {
            @Override
            protected Object createXmlContent(Connection connection) throws Exception
            {
                return Alien.ET.getXml();
            }
        }.execute();
    }

    @Test
    public void insertXmlTypeFromWrongType() throws Exception
    {
        new TestExecutor()
        {
            @Override
            protected Object createXmlContent(Connection connection) throws Exception
            {
                return new Integer(1);
            }

            @Override
            protected void performAsserts(MuleMessage response) throws Exception
            {
                assertThat(response.getExceptionPayload(), is(notNullValue()));

                assertNoAliens();
            }
        }.execute();
    }

    private void assertNoAliens() throws SQLException
    {
        List<Map<String, String>> result = selectData("SELECT name FROM Alien", getDefaultDataSource());
        assertRecords(result);
    }

    protected void assertInsertedAlien() throws SQLException
    {
        List<Map<String, String>> result = selectData("SELECT name FROM Alien", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", Alien.ET.getName())));
    }

    private abstract class TestExecutor
    {
        void execute() throws Exception
        {
            DataSource defaultDataSource = getDefaultDataSource();
            Connection connection = defaultDataSource.getConnection();

            try
            {
                testDatabase.executeUpdate(connection, "DELETE FROM ALIEN");

                Object xmlContent = createXmlContent(connection);

                LocalMuleClient client = muleContext.getClient();

                Map<String, Object> messageProperties = new HashMap<String, Object>();
                messageProperties.put("name", Alien.ET.getName());

                MuleMessage response = client.send("vm://insertXmlType", xmlContent, messageProperties);

                performAsserts(response);
            }
            finally
            {
                if (connection != null)
                {
                    connection.close();
                }
            }
        }

        protected abstract Object createXmlContent(Connection connection) throws Exception;

        protected void performAsserts(MuleMessage response) throws Exception
        {
            assertThat(response.getExceptionPayload(), is(nullValue()));

            assertEquals(1, response.getPayload());

            assertInsertedAlien();
        }
    }
}
