/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.util;

import org.mule.util.IOUtils;

import java.io.IOException;

/**
 * Reads files using {@link org.mule.util.IOUtils}
 */
public class DefaultFileReader implements FileReader
{

    @Override
    public String getResourceAsString(String resourceName) throws IOException
    {
        return IOUtils.getResourceAsString(resourceName, getClass());
    }
}
