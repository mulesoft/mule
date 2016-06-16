/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.ApplicationContextBuilder;

import org.junit.Test;

public class MuleConfigurationConfigTestCase extends AbstractMuleTestCase
{

    @Test
    public void configurationQueueTxLogSizeExplicitValue() throws Exception
    {
        MuleContext muleContext = new ApplicationContextBuilder().setApplicationResources(new String[] {"org/mule/test/config/configuration-queue-tx-log-size-explict-config.xml"}).build();
        assertThat(muleContext.getConfiguration().getQueueTransactionFilesSizeInMegabytes(), is(100));
    }

    @Test
    public void configurationQueueTxLogSizeDefaultValue() throws Exception
    {
        MuleContext muleContext = new ApplicationContextBuilder().setApplicationResources(new String[] {}).build();
        assertThat(muleContext.getConfiguration().getQueueTransactionFilesSizeInMegabytes(), is(500));
    }

}
