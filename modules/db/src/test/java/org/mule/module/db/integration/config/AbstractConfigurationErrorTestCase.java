/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleContext;
import org.mule.tck.junit4.FunctionalTestCase;

public class AbstractConfigurationErrorTestCase extends FunctionalTestCase
{

    private Exception exception;

    public AbstractConfigurationErrorTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        try
        {
            return super.createMuleContext();
        }
        catch (Exception e)
        {
            logger.error("Configuration error detected", e);
            exception = e;
            return null;
        }
    }

    protected final void assertConfigurationError(String assertionMessage)
    {
        assertTrue(assertionMessage, exception != null);
    }

    protected final void assertConfigurationError(String assertionMessage, String expectedError)
    {
        assertConfigurationError(assertionMessage);
        assertThat(exception.getMessage(), containsString(expectedError));
    }
}