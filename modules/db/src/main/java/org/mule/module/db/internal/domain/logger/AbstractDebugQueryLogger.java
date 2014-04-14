/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.logger;

import org.apache.commons.logging.Log;

/**
 * Base class for query loggers that write to a logger
 */
public abstract class AbstractDebugQueryLogger implements QueryLogger
{
    private final Log logger;
    protected final StringBuilder builder;

    protected AbstractDebugQueryLogger(Log logger)
    {
        this.logger = logger;
        this.builder = new StringBuilder();
    }

    @Override
    public void logQuery()
    {
        logger.debug(builder.toString());
    }
}
