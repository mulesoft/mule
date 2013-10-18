/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jbpm;

import org.jbpm.api.Configuration;
import org.jbpm.api.ProcessEngine;

/**
 * Simple FactoryBean for Spring to configure jBPM using defaults.
 */
public class JbpmFactoryBean
{
    public static ProcessEngine buildProcessEngine()
    {
        Configuration config = new Configuration();
        return config.buildProcessEngine();  
    }
}


