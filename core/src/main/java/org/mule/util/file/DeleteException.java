/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.file;

import java.io.File;
import java.io.IOException;

public class DeleteException extends IOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6725758458721277194L;

    public DeleteException()
    {
        super();
    }

    public DeleteException(File f)
    {
        super(f != null ? f.toString() : "null");
    }

}
