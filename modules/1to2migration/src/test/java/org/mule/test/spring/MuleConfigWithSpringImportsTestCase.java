/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

/**
 * Tests a Mule config which loads Spring configs via <import resource="file.xml"/> statements.
 */
public class MuleConfigWithSpringImportsTestCase extends MuleConfigWithSpringConfigsTestCase
{
    public String getConfigResources()
    {
        return "mule-config-with-imports.xml";
    }
    
}


