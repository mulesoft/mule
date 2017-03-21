/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.security.oauth.processor.OAuth2FetchAccessTokenMessageProcessor.CLUSTER_NODE_ID_PROPERTY_KEY;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OAuth2FetchAccessTokenProcessorSpecialRestoreEventTestCase extends OAuth2FetchAccessTokenProcessorRestoreEventTestCase
{

    @Rule
    public SystemProperty clusterIdSystemProperty = new SystemProperty(CLUSTER_NODE_ID_PROPERTY_KEY, "12345");

    @Test
    public void testRestoreOriginalEventWithDefinedClusterId() throws Exception
    {
        eventId = clusterIdSystemProperty.getValue() + "-" + randomUUID;
        oAuth2FetchAccessTokenMessageProcessor.restoreOriginalEvent(event);
        assertThat(processedId, is(eventId));
        assertThat(returnMap.get("state"), is(otherId));
    }

    @Override
    @Before
    public void setUp() throws Exception
    {
        clusterId = clusterIdSystemProperty.getValue();
        super.setUp();
    }

}
