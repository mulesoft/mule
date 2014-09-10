/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.component.simple;

import org.mule.api.MuleEvent;
import org.mule.component.AbstractComponent;

/**
 * <code>PassThroughComponent</code> will simply return the payload back as the
 * result. This typically you don't need to specify this, since it is used by
 * default.
 *
 * Deprecated from 3.6.0.  To achieve the same behaviour simply don't configure this component.
 */
@Deprecated
public class PassThroughComponent extends AbstractComponent
{

    @Override
    protected Object doInvoke(MuleEvent event) throws Exception
    {
        return event.getMessage();
    }

}
