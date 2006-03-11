/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.test.providers.file;

import org.mule.MuleManager;
import org.mule.providers.file.FileMessageAdapter;
import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.provider.UMOMessageAdapter;

import java.io.File;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class FileMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    private File message;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // The working directory is deleted on tearDown
        File dir = new File(MuleManager.getConfiguration().getWorkingDirectory(), "tmp");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        message = File.createTempFile("simple", ".mule", dir);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#getValidMessage()
     */
    public Object getValidMessage()
    {
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#createAdapter()
     */
    public UMOMessageAdapter createAdapter(Object payload) throws Exception
    {
        return new FileMessageAdapter(payload);
    }
}
