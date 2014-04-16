/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.template;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class CdataParameterizedQueryTemplateTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "integration/template/cdata-parameterized-query-template-config.xml";
    }

    @Test
    public void parsesParameterizedQuery() throws Exception
    {
        QueryTemplate queryTemplate = muleContext.getRegistry().lookupObject("parameterizedQuery");
        assertThat(queryTemplate.getSqlText(), equalTo("select * from PLANET"));
    }
}
