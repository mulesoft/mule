/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Exception thrown when a legacy xml file is being processed and it contains configuration violations
 */
public class LegacyXmlException extends IOException
{
    private List warnings = new ArrayList();
    private List errors = new ArrayList();

    public LegacyXmlException(String string)
    {
        super(string);
    }


    public LegacyXmlException(String string, List warnings, List errors)
    {
        super(string);
        this.warnings = warnings;
        this.errors = errors;
    }


    public List getWarnings()
    {
        return warnings;
    }

    public List getErrors()
    {
        return errors;
    }
}
