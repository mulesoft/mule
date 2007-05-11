/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file.test;

import org.mule.providers.file.FilenameParser;
import org.mule.umo.provider.UMOMessageAdapter;

public class DummyFilenameParser implements FilenameParser
{

    public String getFilename(UMOMessageAdapter adapter, String pattern)
    {
        return null;
    }

}
