/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.tomcat;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.context.DefaultMuleContextFactory;

/**
 *
 */
public class MuleTomcatListener implements LifecycleListener {

    private static Log log = LogFactory.getLog(MuleTomcatListener.class);

    protected MuleContext muleContext;

    public void lifecycleEvent(LifecycleEvent event) {
        if (Lifecycle.BEFORE_START_EVENT.equals(event.getType())) {
            if (log.isDebugEnabled()) {
                log.debug("BEFORE_START_EVENT");
            }
            doStart();
            return;
        }

        if (Lifecycle.BEFORE_STOP_EVENT.equals(event.getType())) {
            if (log.isDebugEnabled()) {
                log.debug("BEFORE_STOP_EVENT");
            }
            doStop();
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("not our event: " + event.getType());
        }
    }

    protected void doStart() {
        log.info("Starting Mule");
        DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        try {
            muleContext = muleContextFactory.createMuleContext();
            muleContext.start();
        } catch (Exception e) {
            log.error("Failed to start Mule", e);
        }
    }

    protected void doStop() {
        log.info("Stopping Mule");
        try {
            muleContext.stop();
        } catch (MuleException e) {
            // sigh, ridiculous juli bugs - logger would have already been disposed by a shutdown handler by now
            System.err.println("Failed to stop Mule: " + e);
        }
        muleContext.dispose();
    }
}
