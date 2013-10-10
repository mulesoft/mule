/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
