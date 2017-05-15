/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import org.mule.tck.AbstractConfigurationErrorTestCase;

import org.junit.Test;

public class SelectUnresolvedTextQueryParamTestCase extends AbstractConfigurationErrorTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "integration/select/select-unresolved-text-query-param-config.xml";
    }

    @Test
    public void testRequiresDefinedParameter() throws Exception
    {
        assertConfigurationError("Able to define a query text with a not defined param", "name");
    }

}
