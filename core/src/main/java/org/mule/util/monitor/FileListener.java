/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.monitor;

import java.io.File;
import java.io.IOException;

/**
 * Interface for listening to disk file changes.
 */
public interface FileListener
{
    /**
     * Called when one of the monitored files are created, deleted or modified.
     * 
     * @param file File which has been changed.
     */
    void fileChanged(File file) throws IOException;
}
