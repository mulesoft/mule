/*
 * $Id: $
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

/**
 * Default exception Handler used when executing work in the work manager
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision: $
 */
public class DefaultWorkListener implements WorkListener {

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public void workAccepted(WorkEvent event) {
        handleWorkException(event, "workAccepted");
    }

    public void workRejected(WorkEvent event) {
        handleWorkException(event, "workRejected");
    }

    public void workStarted(WorkEvent event) {
        handleWorkException(event, "workStarted");
    }

    public void workCompleted(WorkEvent event) {
        handleWorkException(event, "workCompleted");
    }

     protected void handleWorkException(WorkEvent event, String type) {
        Throwable e;

        if (event != null && event.getException() != null) {
            e = event.getException();
        }
        else {
            return;
        }

        if (event.getException().getCause() != null) {
            e = event.getException().getCause();
        }

        logger.error("Work caused exception on '" + type + "'. Work being executed was: " + event.getWork().toString());
        logger.error(e);
    }
}
