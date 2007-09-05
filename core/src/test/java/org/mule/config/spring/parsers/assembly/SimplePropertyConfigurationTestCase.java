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

public class SimplePropertyConfigurationTestCase extends AbstractBasePropertyConfigurationTestCase
{

    public static final String SIMPLE = "simple";

    public void testSimple()
    {
        PropertyConfiguration config = new SimplePropertyConfiguration();
        setTestValues(SIMPLE, config);
        verifyTestValues(SIMPLE, config);
        verifyIgnored(SIMPLE, config);
    }

}
