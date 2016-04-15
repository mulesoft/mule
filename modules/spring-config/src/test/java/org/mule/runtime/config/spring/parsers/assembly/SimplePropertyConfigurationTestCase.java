/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.assembly;

import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.assembly.configuration.SimplePropertyConfiguration;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class SimplePropertyConfigurationTestCase extends AbstractBasePropertyConfigurationTestCase
{

    public static final String SIMPLE = "simple";

    @Test
    public void testSimple()
    {
        PropertyConfiguration config = new SimplePropertyConfiguration();
        setTestValues(SIMPLE, config);
        verifyTestValues(SIMPLE, config);
        verifyIgnored(SIMPLE, config);
    }

}
