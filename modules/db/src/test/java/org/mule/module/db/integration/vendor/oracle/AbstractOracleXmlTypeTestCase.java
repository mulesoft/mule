/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.vendor.oracle;

import static org.junit.Assume.assumeThat;
import static org.mule.module.db.integration.DbTestUtil.selectData;
import static org.mule.module.db.integration.TestRecordUtil.assertRecords;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.Alien;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.Record;
import org.mule.module.db.internal.domain.type.oracle.OracleXmlType;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.hamcrest.Description;
import org.junit.Before;
import org.junit.internal.matchers.TypeSafeMatcher;

/**
 * Base class for test that use oracle.xdb.XMLType values.
 * <p/>
 * These test required to include xdb.jar and xmlparserv2.jar libraries into
 * the project.
 */
public abstract class AbstractOracleXmlTypeTestCase extends AbstractDbIntegrationTestCase
{

    public AbstractOracleXmlTypeTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Before
    public void setUp() throws Exception
    {
        assumeThat(this, new TypeSafeMatcher<AbstractOracleXmlTypeTestCase>()
        {
            @Override
            public boolean matchesSafely(AbstractOracleXmlTypeTestCase item)
            {
                try
                {
                    OracleXmlType.getXmlTypeClass();
                    return true;
                }
                catch (ClassNotFoundException e)
                {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText(String.format("Cannot find class %s. Check that required libraries are available", OracleXmlType.ORACLE_XMLTYPE_CLASS));
            }
        });
    }

    protected void assertUpdatedAlienDscription() throws SQLException
    {
        List<Map<String, String>> result = selectData("SELECT name FROM Alien a where a.DESCRIPTION.extract('/Alien/Planet/text()').getStringVal() = 'Mars' ORDER BY NAME", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", Alien.ET.getName())), new Record(new Field("NAME", Alien.MONGUITO.getName())));
    }
}
