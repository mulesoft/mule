/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
