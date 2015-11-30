/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import org.mule.DefaultMuleContext;
import org.mule.api.config.MuleConfiguration;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public class FilePersistenceTestCase extends AbstractTransactionQueueManagerTestCase
{

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Override
    protected TransactionalQueueManager createQueueManager() throws Exception
    {
        TransactionalQueueManager mgr = new TransactionalQueueManager();
        MuleConfiguration mockConfiguration = Mockito.mock(MuleConfiguration.class);
        Mockito.when(mockConfiguration.getWorkingDirectory()).thenReturn(temporaryFolder.getRoot().getAbsolutePath());
        ((DefaultMuleContext)muleContext).setMuleConfiguration(mockConfiguration);
        mgr.setMuleContext(muleContext);
        mgr.initialise();
        mgr.setDefaultQueueConfiguration(new DefaultQueueConfiguration(0, true));
        return mgr;
    }

    @Override
    protected boolean isPersistent()
    {
        return true;
    }
}
