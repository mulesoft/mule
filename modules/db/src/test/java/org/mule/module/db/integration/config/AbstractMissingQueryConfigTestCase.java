/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import org.mule.tck.AbstractConfigurationErrorTestCase;

import org.junit.Test;

public abstract class AbstractMissingQueryConfigTestCase extends AbstractConfigurationErrorTestCase
{

    @Test
    public void doMissingQueryTest()
    {
        String messageProcessorElement = getMessageProcessorElement();
        assertConfigurationError(String.format("Able to define an incomplete %s message processor", messageProcessorElement),
                                 String.format("Element db:%s must contain one of the following elements", messageProcessorElement));
    }

    protected abstract String getMessageProcessorElement();
}
