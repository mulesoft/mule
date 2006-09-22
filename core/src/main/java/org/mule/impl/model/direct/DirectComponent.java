/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.direct;

import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleMessage;
import org.mule.impl.model.AbstractComponent;
import org.mule.impl.model.DefaultMuleProxy;
import org.mule.impl.model.MuleProxy;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.model.UMOModel;

import java.util.List;

/**
 * A direct component invokes the service component directly without any
 * threading or pooling, even when the nvocation is asynchronous
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DirectComponent extends AbstractComponent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8590955440156945732L;

    protected List interceptorList = null;
    protected MuleProxy proxy;

    public DirectComponent(MuleDescriptor descriptor, UMOModel model) {
        super(descriptor, model);
    }

    protected void doInitialise() throws InitialisationException {

        try {
            Object component = lookupComponent();
            proxy = new DefaultMuleProxy(component, descriptor, null);
            proxy.setStatistics(getStatistics());
        } catch (UMOException e) {
            throw new InitialisationException(e, this);
        }
    }

    protected UMOMessage doSend(UMOEvent event) throws UMOException {

        Object obj = proxy.onCall(event);
        if(obj instanceof UMOMessage) {
            return (UMOMessage)obj;
        } else {
            return new MuleMessage(obj, event.getMessage());
        }
    }

    protected void doDispatch(UMOEvent event) throws UMOException {
        proxy.onCall(event);
    }

    protected void doStop() throws UMOException {
        proxy.stop();
    }

    protected void doStart() throws UMOException {
        proxy.start();
    }

    protected void doPause() {
        proxy.suspend();
    }

    protected void doResume() {
        proxy.resume();
    }

    protected void doDispose() {
        proxy.dispose();
    }
}
