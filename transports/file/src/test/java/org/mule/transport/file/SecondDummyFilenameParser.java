/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.mule.api.MuleMessage;

public class SecondDummyFilenameParser extends DummyFilenameParser
{
    @Override
    public String getFilename(MuleMessage message, String pattern)
    {
        return null;
    }
}
