/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.mule.tck.junit4.FunctionalTestCase;

public class QuartzConnectorHotStopRestartTestCase extends FunctionalTestCase
{

    private static final String QUARTZ_CONNECTOR_NAME = "Quartz";

    @Override
    protected String getConfigFile()
    {
        return "quartz-connector-hot-stop-restart-test.xml";
    }

    @Test
    public void testStopRestartConnectorShouldPauseAndRestartJob() throws Exception
    {
        QuartzConnector connector = muleContext.getRegistry().lookupObject(QUARTZ_CONNECTOR_NAME);
        QuartzMessageReceiver receiver = (QuartzMessageReceiver) connector.getReceivers().get("quartz://JustPingIt1");

        // Originally the scheduler must be started / not paused
        assertThat(FALSE, equalTo(connector.getQuartzScheduler().isInStandbyMode()));
        receiver.doStop();
        // when a stop request is sent, the scheduler should be set in stand by mode
        assertThat(TRUE, equalTo(connector.getQuartzScheduler().isInStandbyMode()));
        receiver.doStart();
        // when a start request is performed, the scheduler should exist stand by mode.
        assertThat(FALSE, equalTo(connector.getQuartzScheduler().isInStandbyMode()));
    }
}
