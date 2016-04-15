/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.logger;

import org.mule.module.db.internal.domain.param.InputQueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;

import org.apache.commons.logging.Log;

/**
 * Logs a single query in debug level
 */
public class DebugSingleQueryLogger extends AbstractDebugQueryLogger implements SingleQueryLogger
{

    private final QueryTemplate queryTemplate;

    public DebugSingleQueryLogger(Log logger, QueryTemplate queryTemplate)
    {
        super(logger);

        this.queryTemplate = queryTemplate;

        builder.append("Executing query:\n").append(queryTemplate.getSqlText());

        if (hasParameters())
        {
            builder.append("\nParameters:");
        }
    }

    protected boolean hasParameters()
    {
        return queryTemplate.getInputParams().size() > 0;
    }

    @Override
    public void addParameter(InputQueryParam param, Object value)
    {
        builder.append("\n")
                .append(param.getName() != null ? param.getName() : param.getIndex())
                .append(" = ").append(value);
    }
}
