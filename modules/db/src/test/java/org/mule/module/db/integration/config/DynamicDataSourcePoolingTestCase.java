/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import org.mule.module.db.integration.model.AbstractTestDatabase;

public class DynamicDataSourcePoolingTestCase extends DatasourcePoolingTestCase
{

    public DynamicDataSourcePoolingTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/config/dynamic-derby-pooling-db-config.xml", "integration/config/dynamic-connection-pooling-config.xml"};
    }
}
