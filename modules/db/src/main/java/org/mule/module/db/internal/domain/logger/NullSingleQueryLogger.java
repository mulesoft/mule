/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.logger;

import org.mule.module.db.internal.domain.param.InputQueryParam;

/**
 * Null implementation of {@link SingleQueryLogger}
 */

public class NullSingleQueryLogger extends AbstractNullQueryLogger  implements SingleQueryLogger
{

    @Override
    public void addParameter(InputQueryParam param, Object value)
    {
        // Do nothing
    }
}
