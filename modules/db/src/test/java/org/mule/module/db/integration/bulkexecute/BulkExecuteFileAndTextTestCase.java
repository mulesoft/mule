/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.bulkexecute;

import org.mule.tck.AbstractConfigurationErrorTestCase;

import org.junit.Test;

public class BulkExecuteFileAndTextTestCase extends AbstractConfigurationErrorTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "integration/bulkexecute/bulk-execute-file-and-text-config.xml";
    }

    @Test
    public void doesNotAllowSimultaneousQueryTextAndFile() throws Exception
    {
        assertConfigurationError("Able to define query text and file simultaneously");

    }
}
