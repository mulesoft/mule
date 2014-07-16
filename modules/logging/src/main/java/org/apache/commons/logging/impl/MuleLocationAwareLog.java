/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.apache.commons.logging.impl;

import org.slf4j.spi.LocationAwareLogger;

/**
 * A subclass to open up the acess.
 */
public class MuleLocationAwareLog extends SLF4JLocationAwareLog
{

    public MuleLocationAwareLog(LocationAwareLogger logger)
    {
        super(logger);
    }
}
