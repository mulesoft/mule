/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import java.util.List;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;

/**
 * An object that owns Mule objects and delegates startup/shutdown events to them.
 */
public abstract class AbstractMuleObjectOwner<T> implements Lifecycle, MuleContextAware, FlowConstructAware {

    protected MuleContext muleContext;
    protected FlowConstruct flowConstruct;

    public void setMuleContext(MuleContext context) {
        this.muleContext = context;
    }

    public void setFlowConstruct(FlowConstruct flowConstruct) {
        this.flowConstruct = flowConstruct;
    }

    public MuleContext getMuleContext() {
        return muleContext;
    }

    public FlowConstruct getFlowConstruct() {
        return flowConstruct;
    }

    public void initialise() throws InitialisationException {
        for (T object : getOwnedObjects()) {
            if (object instanceof MuleContextAware) {
                ((MuleContextAware) object).setMuleContext(muleContext);
            }
            if (object instanceof FlowConstructAware) {
                ((FlowConstructAware) object).setFlowConstruct(flowConstruct);
            }
            if (object instanceof Initialisable) {
                ((Initialisable) object).initialise();
            }
        }
    }

    public void dispose() {
        for (T processor : getOwnedObjects()) {

            if (processor instanceof Disposable) {
                ((Disposable) processor).dispose();
            }
        }
    }


    public void start() throws MuleException {

        for (T processor : getOwnedObjects()) {
            if (processor instanceof Startable) {
                ((Startable) processor).start();
            }
        }
    }


    public void stop() throws MuleException {

        for (T processor : getOwnedObjects()) {
            if (processor instanceof Stoppable) {
                ((Stoppable) processor).stop();
            }

        }
    }

    protected abstract List<T> getOwnedObjects();
}
