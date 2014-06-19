/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.logger;

import org.mule.module.db.internal.domain.param.InputQueryParam;

/**
 * Logs a single query
 */
public interface SingleQueryLogger extends QueryLogger
{

    /**
     * Adds the value of a query parameter for logging
     *
     * @param param non null parameter to log
     * @param value value to log
     */
    void addParameter(InputQueryParam param, Object value);
}
