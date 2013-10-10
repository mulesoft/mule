/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
