/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.logger;

import org.apache.commons.logging.Log;

public class DebugBulkQueryLogger extends AbstractDebugQueryLogger implements BulkQueryLogger
{

    public DebugBulkQueryLogger(Log logger)
    {
        super(logger);
        builder.append("Executing bulk query:\n");
    }


    @Override
    public void addQuery(String query)
    {
        builder.append(query).append("\n");
    }
}
