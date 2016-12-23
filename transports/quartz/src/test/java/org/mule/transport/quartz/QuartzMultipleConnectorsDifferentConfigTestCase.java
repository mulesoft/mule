/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.quartz;

import org.junit.Test;

import org.mule.tck.junit4.FunctionalTestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * The following bug must be avoided:
 * When more than one quartz connectors are registered,
 * the quartz scheduler for these connectors have the same name.
 * As a consequence of this, only the properties for the first
 * connector are taken into account as the lookup for the quartz
 * scheduler always returns the same instance (with the same
 * name)
 * <p>
 * see Jira ticket MULE-11159
 */
public class QuartzMultipleConnectorsDifferentConfigTestCase extends FunctionalTestCase
{

    private static String QUARTZ_CONNECTOR_NAME_1 = "Quartz1";

    private static String QUARTZ_CONNECTOR_NAME_2 = "Quartz2";


    public QuartzMultipleConnectorsDifferentConfigTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "quartz-multiple-connectors-different-config-flow.xml";
    }

    @Test
    public void twoConnectorsWithDifferentNamesCreatesSchedulersWithDifferentNames() throws Exception
    {
        QuartzConnector quartzConnector1 = (QuartzConnector) muleContext.getRegistry().lookupConnector(QUARTZ_CONNECTOR_NAME_1);
        QuartzConnector quartzConnector2 = (QuartzConnector) muleContext.getRegistry().lookupConnector(QUARTZ_CONNECTOR_NAME_2);

        assertThat("scheduler-" + muleContext.getConfiguration().getId() + "-" + QUARTZ_CONNECTOR_NAME_1, equalTo(quartzConnector1.getQuartzScheduler().getSchedulerName()));
        assertThat("scheduler-" + muleContext.getConfiguration().getId() + "-" + QUARTZ_CONNECTOR_NAME_2, equalTo(quartzConnector2.getQuartzScheduler().getSchedulerName()));
    }

}
