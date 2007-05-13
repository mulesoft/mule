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

import java.io.OutputStream;
import java.io.IOException;

public class UploadFile extends File
{

    public UploadFile(String name, ServerState state)
    {
        super(name, state);
    }

    public OutputStream createOutputStream(long offset) throws IOException
    {
        return new SignallingOutputStream(getShortName(), getState());
    }

}
