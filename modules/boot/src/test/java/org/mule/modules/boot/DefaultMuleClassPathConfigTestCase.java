/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.boot;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.net.URL;
import java.util.List;

public class DefaultMuleClassPathConfigTestCase extends AbstractMuleTestCase
{

    /**
     * $MULE_BASE/lib/user folder should come before $MULE_HOME/lib/user. Note this
     * test checks folder only, not the jars. See
     * http://mule.mulesource.org/jira/browse/MULE-1311 for more details.
     * 
     * @throws Exception in case of any error
     */
    public void testMuleBaseUserFolderOverridesMuleHome() throws Exception
    {
        final File tempDir = SystemUtils.getJavaIoTmpDir();
        final long now = System.currentTimeMillis();
        final File currentTestFolder = new File(tempDir, "mule_test_delete_me_" + now);

        File testMuleHome = new File(currentTestFolder, "mule_home");
        File testMuleBase = new File(currentTestFolder, "mule_base");

        try
        {
            assertTrue("Couldn't create test Mule home folder.", testMuleHome.mkdirs());
            assertTrue("Couldn't create test Mule base folder.", testMuleBase.mkdirs());

            DefaultMuleClassPathConfig cp = new DefaultMuleClassPathConfig(testMuleHome, testMuleBase);
            List urls = cp.getURLs();
            assertNotNull("Urls shouldn't be null.", urls);
            assertFalse("Urls shouldn't be empty.", urls.isEmpty());

            URL muleBaseUserFolder = new File(testMuleBase, DefaultMuleClassPathConfig.USER_DIR)
                .getAbsoluteFile().toURI().toURL();
            String expectedMuleBaseUserFolder = muleBaseUserFolder.toExternalForm();
            String firstUrl = ((URL) urls.get(0)).toExternalForm();
            assertEquals("$MULE_BASE/lib/user must come first.", expectedMuleBaseUserFolder, firstUrl);
        }
        finally
        {
            // tearDown() may be too late for these calls
            FileUtils.deleteTree(currentTestFolder);
        }
    }

}
