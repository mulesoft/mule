/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.scripting;

import junit.framework.TestCase;

import org.mule.MuleManager;
import org.mule.MuleServer;

public abstract class SimpleRunner extends TestCase
 {
    protected String config = null;

    public SimpleRunner(String config) {
        this.config = config;
    }

    public static boolean serverIsStarted()
    {
        if (MuleManager.isInstanciated()) {
            return MuleManager.getInstance().isStarted();
        } else {
            return false;
        }
    }

    public void testRunner() throws Exception {
        runSample();
    }

    protected void setUp() throws Exception {
        MuleServer server = new MuleServer(config);
        server.start(true);

        // let server start up
        while (!serverIsStarted()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    protected void tearDown() throws Exception {
        MuleManager.getInstance().dispose();
    }

    protected void log(String msg) {
        System.out.println(msg);
    }

    protected void log(String msg, Throwable t) {
        System.err.println(msg + ": " + t.getMessage());
        t.printStackTrace(System.err);
    }

    protected abstract void runSample() throws Exception;
}
