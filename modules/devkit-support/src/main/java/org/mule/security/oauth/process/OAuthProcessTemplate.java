/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth.process;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.ProcessTemplate;
import org.mule.api.capability.Capabilities;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.common.security.oauth.OAuthAdapter;
import org.mule.security.oauth.callback.ProcessCallback;

public class OAuthProcessTemplate<P> implements ProcessTemplate<P, Capabilities>
{

    private final Capabilities object;

    public OAuthProcessTemplate(Capabilities object)
    {
        this.object = object;
    }

    public P execute(ProcessCallback<P, Capabilities> processCallback,
                     MessageProcessor messageProcessor,
                     MuleEvent event) throws Exception
    {
        if (processCallback.isProtected())
        {
            ((OAuthAdapter) object).hasBeenAuthorized();
        }
        return processCallback.process(object);
    }

    public P execute(ProcessCallback<P, Capabilities> processCallback, Filter filter, MuleMessage message)
        throws Exception
    {
        if (processCallback.isProtected())
        {
            ((OAuthAdapter) object).hasBeenAuthorized();
        }
        return processCallback.process(object);
    }

}
