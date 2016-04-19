/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers;

import org.junit.Test;

public class MuleElementRequiredTestCase extends AbstractBadConfigTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "mule-element-required-test.xml";
    }

    @Test
    public void testHelpfulErrorMessage() throws Exception
    {
        assertErrorContains("This element should be embedded");
    }

}
