/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.module.launcher.artifact.LeakCleaner;

public class TestLeakCleaner implements LeakCleaner
{

    private ClassLoader classLoader;

    @Override
    public void clean()
    {
        classLoader = this.getClass().getClassLoader();
    }

    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

}

