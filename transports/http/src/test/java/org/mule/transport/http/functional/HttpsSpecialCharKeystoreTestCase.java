/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import org.mule.util.IOUtils;

import java.io.File;

import org.apache.commons.io.FileUtils;

public class HttpsSpecialCharKeystoreTestCase extends HttpsFunctionalTestCase
{

    public HttpsSpecialCharKeystoreTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        super.doSetUpBeforeMuleContextCreation();
        File serverKeystore = new File(IOUtils.getResourceAsUrl(SERVER_KEYSTORE, getClass()).getPath());
        File serverKeystoreCopy = new File(workingDirectory.getRoot().getPath() + "/dir$/serverKeystore");
        FileUtils.copyFile(serverKeystore, serverKeystoreCopy);
        //this is a SystemProperty rule defined in the superclass
        System.setProperty(SERVER_KEYSTORE_PATH, serverKeystoreCopy.getPath());
    }

}
