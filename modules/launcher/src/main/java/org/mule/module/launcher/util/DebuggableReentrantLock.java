/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher.util;

import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public class DebuggableReentrantLock extends ReentrantLock
{

    public DebuggableReentrantLock()
    {
    }

    public DebuggableReentrantLock(boolean fair)
    {
        super(fair);
    }

    @Override
    public Thread getOwner()
    {
        return super.getOwner();
    }
}
