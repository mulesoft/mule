/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.vendor.oracle;

import static org.junit.Assert.assertEquals;
import static org.mule.module.db.integration.DbTestUtil.selectData;
import static org.mule.module.db.integration.TestRecordUtil.assertRecords;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.Alien;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.Record;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class OracleUpdateXmlTypeTestCase extends AbstractOracleXmlTypeTestCase
{

    public OracleUpdateXmlTypeTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/vendor/oracle/oracle-update-xml-type-config.xml"};
    }

    @Test
    public void updateXmlTyeColumn() throws Exception
    {
        DataSource defaultDataSource = getDefaultDataSource();
        Connection connection = defaultDataSource.getConnection();

        Object xmlType;
        try
        {
            xmlType = XmlTypeUtils.createXmlType(connection, Alien.ET.getXml());

        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }

        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://updateWithXmlTypeParam", xmlType, null);

        assertEquals(2, response.getPayload());

        assertUpdatedAlienDscription();
    }
}