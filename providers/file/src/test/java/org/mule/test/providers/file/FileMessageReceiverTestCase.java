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

import com.mockobjects.dynamic.Mock;

import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.file.FileMessageReceiver;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.providers.AbstractMessageReceiverTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageReceiver;

import java.io.File;

/**
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class FileMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{

    File read = new File("testcasedata/read");
    File move = new File("testcasedata/move");
    Mock session = MuleTestUtils.getMockSession();

    public void testReceiver() throws Exception
    {
        // FIX A bit hard testing receive from a unit simple as we need to reg
        // listener etc
        // file endpoint functiona tests for this
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageReceiverTestCase#getMessageReceiver()
     */
    public UMOMessageReceiver getMessageReceiver() throws Exception
    {
        endpoint.getConnector().startConnector();
        Mock mockComponent = new Mock(UMOComponent.class);
        read.deleteOnExit();
        move.deleteOnExit();

        return new FileMessageReceiver(endpoint.getConnector(),
                                       (UMOComponent) mockComponent.proxy(),
                                       endpoint,
                                       read.getAbsolutePath(),
                                       move.getAbsolutePath(),
                                       null,
                                       new Long(1000));
    }



    public UMOEndpoint getEndpoint() throws Exception
    {
        return new MuleEndpoint("file://./simple", true);
    }
}
