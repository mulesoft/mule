/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.domain.db;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.functional.junit4.DomainFunctionalTestCase;
import org.mule.tck.MuleTestUtils;

import org.junit.Test;

public class DomainTransformerRegistrationTestCase extends DomainFunctionalTestCase
{

    private static final String APP1 = "app1";
    private static final String APP2 = "app2";
    private static final String[] APPLICATION_RESOURCES = new String[] {"domain/domain-transformer-registration-config.xml"};

    @Override
    protected String getDomainConfig()
    {
        return "domain/empty-domain-config.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {new ApplicationConfig(APP1, APPLICATION_RESOURCES),
                new ApplicationConfig(APP2, APPLICATION_RESOURCES)
        };
    }

    @Test
    public void registerTransformerOnce() throws Exception
    {
        final MuleContext app1Context = getMuleContextForApp(APP1);
        Flow flow = (Flow) app1Context.getRegistry().lookupFlowConstruct("main");
        final MuleMessage response = flow.process(MuleTestUtils.getTestEvent(TRUE.toString(), MessageExchangePattern.REQUEST_RESPONSE, app1Context)).getMessage();

        assertThat(response.getPayload(), instanceOf(Boolean.class));
    }
}
