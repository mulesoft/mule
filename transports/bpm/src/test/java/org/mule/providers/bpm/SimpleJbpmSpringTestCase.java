/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.bpm;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.umo.UMOManagementContext;

/**
 * Tests the connector against jBPM with 2 simple processes.
 * jBPM is instantiated by Spring using the Spring jBpm module.
 */
public class SimpleJbpmSpringTestCase extends SimpleJbpmTestCase {

    protected ConfigurationBuilder getBuilder() throws Exception {
        return new MuleXmlConfigurationBuilder();
    }

    protected String getConfigResources() {
        return "jbpm-spring-config.xml";
    }

    protected void doSetupManager(UMOManagementContext context) throws Exception {
        // Disable parent method
    }

    protected void doSetUp() throws Exception {
        connector =
            (ProcessConnector) managementContext.getRegistry().lookupConnector("jBpmConnector");
        bpms = connector.getBpms();
        connector.setAllowGlobalReceiver(true);
    }
}
