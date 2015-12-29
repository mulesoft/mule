/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.domain.registry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.mule.api.MuleContext;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.construct.Flow;
import org.mule.endpoint.AbstractEndpoint;
import org.mule.functional.junit4.DomainFunctionalTestCase;

import org.junit.Test;

public class DomainEndpointCreationTestCase extends DomainFunctionalTestCase
{

    private static final String APP = "app";
    private static final String[] APPLICATION_RESOURCES = new String[] {"domain/app-jms-endpoints-config.xml"};

    @Override
    protected String getDomainConfig()
    {
        return "domain/jms-shared-connector.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {new ApplicationConfig(APP, APPLICATION_RESOURCES),
        };
    }

    @Test
    public void registerTransformerOnce() throws Exception
    {
        final MuleContext appContext = getMuleContextForApp(APP);

        Flow flow = appContext.getRegistry().lookupObject("main");

        final MessageSource messageSource = flow.getMessageSource();
        assertThat(((AbstractEndpoint) messageSource).getMuleContext(), equalTo(appContext));

        final MessageProcessor messageProcessor = flow.getMessageProcessors().get(0);
        assertThat(((AbstractEndpoint) messageProcessor).getMuleContext(), equalTo(appContext));
    }
}
