/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.streaming;

import java.io.IOException;
import java.io.InputStream;

/**
 * A simple bridge service for testing entry point resolution
 */
public class SimpleStreamingBean
{
    public InputStream doit(InputStream in) throws IOException
    {
        return in;
    }
}
