/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.issues;

import org.mule.api.transport.Connector;
import org.mule.transport.email.AbstractRetrieveMailConnector;
import org.mule.transport.email.connectors.ImapConnectorTestCase;
import org.mule.util.FileUtils;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractBaseBackupMule2050TestCase extends ImapConnectorTestCase
{

    private boolean backupEnabled;

    public AbstractBaseBackupMule2050TestCase(boolean backupEnabled)
    {
        this.backupEnabled = backupEnabled;
    }

    @Override
    public Connector createConnector() throws Exception
    {
        Connector connector = super.createConnector();
        ((AbstractRetrieveMailConnector) connector).setBackupEnabled(backupEnabled);
        return connector;
    }

    @Override
    public void testReceiver() throws Exception
    {
        File dir = FileUtils.newFile(muleContext.getConfiguration().getWorkingDirectory() + "/mail/INBOX");
        FileUtils.deleteTree(new File(muleContext.getConfiguration().getWorkingDirectory() + "/mail"));
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
