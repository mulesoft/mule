/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.MySqlTestDatabase;

import java.util.Collections;
import java.util.List;

import org.junit.runners.Parameterized;

public class DynamicMysqlConfigTestCase extends AbstractDynamicDatabaseConfigTestCase
{

    public DynamicMysqlConfigTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        if (TestDbConfig.getMySqlResource().isEmpty())
        {
            return Collections.emptyList();
        }
        else
        {
            return Collections.singletonList(new Object[] {"integration/config/mysql-dynamic-config.xml", new MySqlTestDatabase()});
        }
    }

}
