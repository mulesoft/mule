/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.bpm.jbpm;

import org.mule.config.ConfigurationBuilder;
import org.mule.extras.spring.config.SpringConfigurationBuilder;

/**
 * Tests the connector against jBPM with a simple process.
 * jBPM is instantiated by Spring using the Spring jBPM module.
 */
public class SimpleJbpmSpringTestCase extends SimpleJbpmTestCase {

    protected ConfigurationBuilder getBuilder() throws Exception {
        return new SpringConfigurationBuilder();
    }

    protected String getConfigResources() {
        return "mule-jbpm-spring-config.xml";
    }
}
