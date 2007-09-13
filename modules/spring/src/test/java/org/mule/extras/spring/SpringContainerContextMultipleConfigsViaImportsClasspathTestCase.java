/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring;



/**
 * Tests the Spring container where more than one config file are loaded via
 * <code>&lt;import resource="classpath:config.xml"/&gt;</code>
 * "classpath:" is a Spring pseudo-URL which clarifies that the resource is to be loaded from the classpath, not the file system. 
 */
public class SpringContainerContextMultipleConfigsViaImportsClasspathTestCase extends SpringContainerContextMultipleConfigsViaImportsTestCase
{
    public String getConfigResources()
    {
        return "spring-imports-classpath.xml";
    }
}
