/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.file.comparator;

import org.mule.util.FileUtils;

import java.io.File;
import java.util.Comparator;

public class TestComparator implements Comparator
{
    public int compare(Object o1, Object o2)
    {
        if (o1 instanceof File && o2 instanceof File)
        {
            File f = (File) o1;
            File f1 = (File) o2;
            if (FileUtils.isFileNewer(f, f1))
            {
                return -1;
            }
            else
            {
                return 1;
            }
        }
        throw new IllegalArgumentException("Compare not file instance");
    }
}
