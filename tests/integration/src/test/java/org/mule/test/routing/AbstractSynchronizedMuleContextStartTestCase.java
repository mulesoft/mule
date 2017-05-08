/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public abstract class AbstractSynchronizedMuleContextStartTestCase extends FunctionalTestCase
{

    protected static volatile int processedMessageCounter = 0;
    protected static final Latch waitMessageInProgress = new Latch();

    public AbstractSynchronizedMuleContextStartTestCase()
    {
        setStartContext(false);
    }

    @Test
    public void waitsForStartedMuleContextBeforeAttemptingToSendMessageToEndpoint() throws Exception
    {
        prePopulateObjectStore();

        muleContext.start();

        Prober prober = new PollingProber(RECEIVE_TIMEOUT, 50);

        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                return processedMessageCounter == 1;
            }

            public String describeFailure()
            {
                return "Did not wait for mule context started before attempting to process event";
            }
        });
    }

    private void prePopulateObjectStore() throws ObjectStoreException
    {
        ObjectStore<MuleEvent> objectStore = muleContext.getRegistry().lookupObject("objectStore");

        DefaultMuleMessage testMessage = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        Flow clientFlow = muleContext.getRegistry().get("flow2");
        DefaultMuleEvent testMuleEvent = new DefaultMuleEvent(testMessage, MessageExchangePattern.REQUEST_RESPONSE, clientFlow);
        objectStore.store(testMuleEvent.getId(), testMuleEvent);
    }

    public static class TestMessageProcessor {

        public String count(String value) throws InterruptedException {
            if (waitMessageInProgress.await(0, TimeUnit.MILLISECONDS)) {
                processedMessageCounter++;
            }

            return value;
        }
    }

}
