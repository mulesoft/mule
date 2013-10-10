/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;

public class DummyFilenameParser implements FilenameParser
{
    public String getFilename(MuleMessage message, String pattern)
    {
        return null;
    }

    public void setMuleContext(MuleContext context)
    {
        // ignore muleContext here
    }
}
