/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import org.junit.Test;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.tck.functional.InvocationCountMessageProcessor.getNumberOfInvocationsFor;

public class UntilSuccessfulAsyncEventTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "until-successful-with-poll-test.xml";
    }

    @Test
    public void executeAsynchronouslyDoingRetries() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("poll-until-successfulFlow");
        flow.start();
        new PollingProber(10000, 1000).check(new Probe()
        {
            public boolean isSatisfied()
            {
                return getNumberOfInvocationsFor("untilSuccessful") == 6;
            }

            public String describeFailure()
            {
                return "INVOCATIONS: " + getNumberOfInvocationsFor("untilSuccessful");
            }
        });
        assertThat(getNumberOfInvocationsFor("exceptionStrategy"), is(1));
    }
}
