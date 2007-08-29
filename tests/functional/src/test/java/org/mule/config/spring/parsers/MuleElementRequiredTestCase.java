/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

public class MuleElementRequiredTestCase extends AbstractBadConfigTestCase
{

    protected String getConfigResources()
    {
        return "mule-element-required-test.xml";
    }

    public void testHelpfulErrorMessage() throws Exception
    {
        assertErrorContains("This element should be embedded");
    }

}
