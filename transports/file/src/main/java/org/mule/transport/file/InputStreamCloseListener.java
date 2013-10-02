/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import java.io.File;

/**
 * Listener to be called once a file gets closed
 */
public interface InputStreamCloseListener
{

    /**
     * Call when a file input stream gets closed
     *
     * @param file the file on which close was call
     */
    void fileClose(File file);
}
