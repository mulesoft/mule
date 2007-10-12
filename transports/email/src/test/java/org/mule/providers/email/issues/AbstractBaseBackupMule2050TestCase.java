/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.issues;

import org.mule.providers.email.AbstractRetrieveMailConnector;
import org.mule.providers.email.connectors.ImapConnectorTestCase;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.FileUtils;

import java.io.File;

public abstract class AbstractBaseBackupMule2050TestCase extends ImapConnectorTestCase
{

    private boolean backupEnabled;

    public AbstractBaseBackupMule2050TestCase(int port, boolean backupEnabled)
    {
        super(port);
        this.backupEnabled = backupEnabled;
    }

    // @Override
    public UMOConnector createConnector() throws Exception
    {
        UMOConnector connector = super.createConnector();
        ((AbstractRetrieveMailConnector) connector).setBackupEnabled(backupEnabled);
        return connector;
    }

    public void testReceiver() throws Exception
    {
        File dir = FileUtils.newFile(managementContext.getRegistry().getConfiguration().getWorkingDirectory() + "/mail/INBOX");
        assertFalse("Mail backup file already exists: " + dir.getAbsolutePath(), dir.exists());
        debug(dir);
        super.testReceiver();
        debug(dir);
        assertTrue(dir.getAbsolutePath(), dir.exists() == backupEnabled);
    }

    protected void debug(File dir)
    {
        logger.debug(dir.getAbsolutePath() + " exists? " + dir.exists());
    }

}