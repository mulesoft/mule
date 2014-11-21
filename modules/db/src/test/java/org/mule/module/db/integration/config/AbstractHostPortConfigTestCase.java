/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.integration.config;

import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;

public abstract class AbstractHostPortConfigTestCase extends AbstractDatabaseConfigTestCase
{

    @Rule
    public SystemProperty databasePort = new SystemProperty("database.port", getDatabasePortPropertyValue());

    protected abstract String getDatabasePortPropertyValue();

    public AbstractHostPortConfigTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }
}
