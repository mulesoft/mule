/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.assembly;

public class ReusablePropertyConfigurationTestCase extends AbstractBasePropertyConfigurationTestCase
{

    public static final String REFERENCE = "reference";
    public static final String WRAPPER = "wrapper";

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
