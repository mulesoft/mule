/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
