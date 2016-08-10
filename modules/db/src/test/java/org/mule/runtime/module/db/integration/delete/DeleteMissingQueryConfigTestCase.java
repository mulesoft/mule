/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.delete;

import org.mule.runtime.module.db.integration.config.AbstractMissingQueryConfigTestCase;

public class DeleteMissingQueryConfigTestCase extends AbstractMissingQueryConfigTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "integration/delete/delete-missing-query-config.xml";
    }

    @Override
    protected String getMessageProcessorElement()
    {
        return "delete";
    }
}
