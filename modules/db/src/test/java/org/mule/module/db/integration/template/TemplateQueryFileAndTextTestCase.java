/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.template;

import org.mule.tck.AbstractConfigurationErrorTestCase;

import org.junit.Test;

public class TemplateQueryFileAndTextTestCase extends AbstractConfigurationErrorTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "integration/template/template-query-file-and-text-config.xml";
    }

    @Test
    public void doesNotAllowSimultaneousQueryTextAndFile() throws Exception
    {
        assertConfigurationError("Able to define a template query with text content and file simultaneously");

    }
}
