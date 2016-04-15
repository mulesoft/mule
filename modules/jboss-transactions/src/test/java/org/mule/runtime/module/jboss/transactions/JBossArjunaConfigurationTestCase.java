/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jboss.transactions;

import static org.junit.Assert.assertTrue;

import com.arjuna.ats.arjuna.common.arjPropertyManager;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JBossArjunaConfigurationTestCase extends AbstractJbossArjunaConfigurationTestCase
{

    private static final String TEMP_OBJECTSTORE_DIR_PROPERTY = "objectstore.dir";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File objectStoreFolder;
    private String previousTempObjectDirProperty;

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        objectStoreFolder = temporaryFolder.newFolder("os_dir");
        previousTempObjectDirProperty = System.getProperty(TEMP_OBJECTSTORE_DIR_PROPERTY);
        System.setProperty(TEMP_OBJECTSTORE_DIR_PROPERTY, objectStoreFolder.getAbsolutePath());
        super.doSetUpBeforeMuleContextCreation();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();

        if (previousTempObjectDirProperty != null)
        {
            System.setProperty(TEMP_OBJECTSTORE_DIR_PROPERTY, previousTempObjectDirProperty);
        }
        else
        {
            System.clearProperty(TEMP_OBJECTSTORE_DIR_PROPERTY);
        }

        previousTempObjectDirProperty = null;
    }

    @Override
    protected String getConfigResources()
    {
        return "jbossts-configuration.xml";
    }

    @Test
    public void testConfiguration()
    {
        assertTransactionManagerPresent();

        assertTrue(arjPropertyManager.getCoordinatorEnvironmentBean().getTxReaperTimeout() == 108000);
        assertTrue(arjPropertyManager.getCoordinatorEnvironmentBean().getDefaultTimeout() == 47);

        assertObjectStoreDir(objectStoreFolder.getAbsolutePath(), muleContext.getConfiguration().getWorkingDirectory());
    }
}
