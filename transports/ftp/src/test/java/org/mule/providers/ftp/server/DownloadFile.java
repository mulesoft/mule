/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DownloadFile extends File
{

    public DownloadFile(String name, ServerState state)
    {
        super(name, state);
    }

    public InputStream createInputStream(long offset) throws IOException
    {
        NamedPayload payload = getState().getDownload(getShortName());
        if (null == payload)
        {
            return null;
        }
        else
        {
            return new NonSignallingInputStream(new ByteArrayInputStream(payload.getPayload()));
        }
    }

}
