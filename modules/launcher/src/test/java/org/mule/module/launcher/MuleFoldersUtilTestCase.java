/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import org.mule.api.config.MuleProperties;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class MuleFoldersUtilTestCase extends AbstractMuleTestCase
{

    public static final String TEST_APP = "testApp";

    @Rule
    public TemporaryFolder muleHome = new TemporaryFolder();

    @Test
    public void getsMuleHome() throws Exception
    {
        doFolderTest(new MuleTestUtils.TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                File folder = MuleFoldersUtil.getMuleHomeFolder();
                assertThat(folder.getAbsolutePath(), equalTo(muleHome.getRoot().getAbsolutePath()));
            }
        });
    }

    @Test
    public void getsAppsFolder() throws Exception
    {
        doFolderTest(new MuleTestUtils.TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                File folder = MuleFoldersUtil.getAppsFolder();
                assertThat(folder.getAbsolutePath(), equalTo(getAppsFolder()));
            }
        });
    }

    @Test
    public void getsAppFolder() throws Exception
    {
        doFolderTest(new MuleTestUtils.TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                File folder = MuleFoldersUtil.getAppFolder(TEST_APP);
                assertThat(folder.getAbsolutePath(), equalTo(getTestAppFolder()));
            }
        });
    }

    @Test
    public void getsAppLibFolder() throws Exception
    {
        doFolderTest(new MuleTestUtils.TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                File folder = MuleFoldersUtil.getAppLibFolder(TEST_APP);
                assertThat(folder.getAbsolutePath(), equalTo(getTestAppFolder() + File.separator + MuleFoldersUtil.LIB_FOLDER));
            }
        });
    }

    @Test
    public void getsAppTempFolder() throws Exception
    {
        doFolderTest(new MuleTestUtils.TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                File folder = MuleFoldersUtil.getAppTempFolder(TEST_APP);
                assertThat(folder.getAbsolutePath(), equalTo(getMuleExecutionFolder() + File.separator + TEST_APP));
            }
        });
    }

    @Test
    public void getsMuleExecutionFolder() throws Exception
    {
        doFolderTest(new MuleTestUtils.TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                File folder = MuleFoldersUtil.getExecutionFolder();
                assertThat(folder.getAbsolutePath(), equalTo(getMuleExecutionFolder()));
            }
        });
    }

    @Test
    public void getsMuleLibFolder() throws Exception
    {
        doFolderTest(new MuleTestUtils.TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                File folder = MuleFoldersUtil.getMuleLibFolder();
                assertThat(folder.getAbsolutePath(), equalTo(muleHome.getRoot().getAbsolutePath() + File.separator + MuleFoldersUtil.LIB_FOLDER));
            }
        });
    }

    private String getMuleExecutionFolder()
    {
        return muleHome.getRoot().getAbsolutePath() + File.separator + MuleFoldersUtil.EXECUTION_FOLDER;
    }

    private void doFolderTest(MuleTestUtils.TestCallback callback) throws Exception
    {
        MuleTestUtils.testWithSystemProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, muleHome.getRoot().getAbsolutePath(), callback);
    }

    private String getAppsFolder()
    {
        return muleHome.getRoot().getAbsolutePath() + File.separator + MuleFoldersUtil.APPS_FOLDER;
    }

    private String getTestAppFolder()
    {
        return getAppsFolder() + File.separator + TEST_APP;
    }
}