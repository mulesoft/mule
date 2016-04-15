/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import org.mule.module.db.integration.model.AbstractTestDatabase;

public class SelectNamedParamParameterizedQueryTestCase extends SelectParameterizedQueryTestCase
{

    public SelectNamedParamParameterizedQueryTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/select/select-named-param-parameterized-query-config.xml"};
    }
}
