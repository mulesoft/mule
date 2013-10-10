/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.assembly;

import org.mule.config.spring.parsers.assembly.configuration.ReusablePropertyConfiguration;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class ReusablePropertyConfigurationTestCase extends AbstractBasePropertyConfigurationTestCase
{

    public static final String REFERENCE = "reference";
    public static final String WRAPPER = "wrapper";

    @Test
    public void testReusable()
    {
        ReusablePropertyConfiguration config = new ReusablePropertyConfiguration();
        setTestValues(REFERENCE, config); // as normal
        verifyTestValues(REFERENCE, config); // transparent wrapper
        verifyIgnored(REFERENCE, config);
        config.reset();
        verifyTestValues(REFERENCE, config); // original values still visible via wrapper
        setTestValues(WRAPPER, config); // add extra values
        verifyTestValues(REFERENCE, config); // original values still visible via wrapper
        verifyTestValues(WRAPPER, config); // new values also visible via wrapper
        verifyIgnored(WRAPPER, config);
        config.reset();
        verifyMissing(WRAPPER, config); // new values deleted
        verifyTestValues(REFERENCE, config); // original values still visible via wrapper
        setTestValues(WRAPPER, config); // add extra values
        verifyTestValues(REFERENCE, config); // original values still visible via wrapper
        verifyTestValues(WRAPPER, config); // new values also visible via wrapper
        config.reset();
        verifyMissing(WRAPPER, config); // new values deleted
        verifyTestValues(REFERENCE, config); // original values still visible via wrapper
    }

}
