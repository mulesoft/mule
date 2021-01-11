/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.api.MessagingException;
import org.mule.construct.Flow;
import org.mule.module.ws.consumer.WSConsumer;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.api.LocatedMuleException.INFO_LOCATION_KEY;

/**
 * This test case verifies that a HTTP Requester that fails is correctly caught and handled by
 * the WSConsumerConfig, placing the WSConsumer Message Processor as the element path on the exception info.
 */
public class WSConsumerHttpRequesterFailureTestCase extends AbstractWSConsumerFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "ws-consumer-http-requester-failure-test-case.xml";
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        // Change default behavior of AbstractWSConsumerFunctionalTestCase as this test only uses the new connector.
        return Arrays.asList(new Object[][] { new Object[]{false} });
    }

    @Test
    public void invalidHttpRequestConfigThrowsExceptionWithElementPath() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("clientInvalidHttpRequestConfig");

        try
        {
            flow.process(getTestEvent(ECHO_REQUEST));
            fail();
        }
        catch (MessagingException ex)
        {
            String element = ex.getInfo().get(INFO_LOCATION_KEY).toString();

            assertThat(element, not(nullValue()));
            assertThat(element, not(isEmptyString()));

            assertThat(ex.getFailingMessageProcessor(), is(instanceOf(WSConsumer.class)));

            QName sourceFileLineQName =
                    new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileLine");
            assertTrue(flow.getAnnotations().containsKey(sourceFileLineQName));
        }
    }

}