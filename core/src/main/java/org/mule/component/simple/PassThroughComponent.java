/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.component.simple;

import org.mule.api.MuleEvent;
import org.mule.component.AbstractComponent;

/**
 * <code>PassThroughComponent</code> will simply return the payload back as the
 * result. This typically you don't need to specify this, since it is used by
 * default.
 */
public class PassThroughComponent extends AbstractComponent
{

    @Override
    protected Object doInvoke(MuleEvent event) throws Exception
    {
        return event.getMessage();
    }

}
