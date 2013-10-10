/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.probe.file;

import org.mule.tck.probe.Probe;

import java.io.File;

/**
 * Checks that a given file is not present in the file system
 */
public class FileDoesNotExists implements Probe
{

    private final File target;

    public FileDoesNotExists(File target)
    {
        this.target = target;
    }

    public boolean isSatisfied()
    {
        return !target.exists();
    }

    public String describeFailure()
    {
        return String.format("File '%s' was not expected to exist", target.getName());
    }
}
