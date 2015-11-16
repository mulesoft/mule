/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.tck.junit4.ExtensionsFunctionalTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import org.junit.Test;

public class DynamicConfigExpirationTestCase extends ExtensionsFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "dynamic-config-expiration.xml";
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {HeisenbergExtension.class};
    }

    @Test
    public void expireDynamicConfig() throws Exception
    {
        final String myName = "Walt";
        final MuleEvent event = getTestEvent(myName);
        String returnedName = runFlow("dynamic", event).getMessage().getPayloadAsString();

        HeisenbergExtension config = (HeisenbergExtension) muleContext.getExtensionManager().getConfiguration("heisenberg", event).getValue();

        // validate we actually hit the correct dynamic config
        assertThat(returnedName, is(myName));
        assertThat(config.getPersonalInfo().getName(), is(myName));

        PollingProber prober = new PollingProber(5000, 1000);
        prober.check(new JUnitProbe()
        {
            @Override
            protected boolean test() throws Exception
            {
                assertThat(config.getStop(), is(1));
                assertThat(config.getDispose(), is(1));

                return true;
            }

            @Override
            public String describeFailure()
            {
                return "config was not stopped or disposed";
            }
        });

        assertThat(config.getInitialise(), is(1));
        assertThat(config.getStart(), is(1));
    }
}
