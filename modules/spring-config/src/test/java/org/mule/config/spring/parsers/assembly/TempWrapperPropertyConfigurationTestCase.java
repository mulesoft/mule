/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.assembly;

import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.assembly.configuration.SimplePropertyConfiguration;
import org.mule.config.spring.parsers.assembly.configuration.TempWrapperPropertyConfiguration;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class TempWrapperPropertyConfigurationTestCase extends AbstractBasePropertyConfigurationTestCase
{

    public static final String REFERENCE = "reference";
    public static final String WRAPPER = "wrapper";

    @Test
    public void testTempWrapper()
    {
        PropertyConfiguration reference = new SimplePropertyConfiguration();
        setTestValues(REFERENCE, reference); // as normal
        PropertyConfiguration wrapper = new TempWrapperPropertyConfiguration(reference);
        verifyTestValues(REFERENCE, wrapper); // transparent wrapper
        setTestValues(WRAPPER, wrapper); // add extra values
        verifyTestValues(REFERENCE, wrapper); // original values still visible via wrapper
        verifyTestValues(WRAPPER, wrapper); // new values also visible via wrapper
        verifyMissing(WRAPPER, reference); // new values not in reference
        verifyTestValues(REFERENCE, reference); // reference values still ok
    }

}
