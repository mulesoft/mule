/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;

import org.apache.abdera.protocol.Resolver;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.Target;
import org.apache.abdera.protocol.server.TargetBuilder;
import org.apache.abdera.protocol.server.WorkspaceManager;
import org.apache.abdera.protocol.server.RequestContext.Scope;
import org.apache.abdera.protocol.server.context.EmptyResponseContext;
import org.apache.abdera.protocol.server.impl.AbstractProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MuleProvider extends AbstractProvider
{
    private final static Log log = LogFactory.getLog(MuleProvider.class);

    public ResponseContext request(RequestContext request)
    {
        MuleEventContext ctx = (MuleEventContext)
                request.getAttribute(Scope.REQUEST, AbderaServiceComponent.EVENT_CONTEXT);

        try
        {
            MuleMessage requestMessage = new DefaultMuleMessage(request, ctx.getMuleContext());
            MuleMessage res = ctx.sendEvent(requestMessage);

            return (ResponseContext) res.getPayload();
        }
        catch (MuleException e)
        {
            log.error(e);
            return new EmptyResponseContext(500);
        }
    }

    @Override
    protected TargetBuilder getTargetBuilder(RequestContext request)
    {
        return null;
    }

    @Override
    protected Resolver<Target> getTargetResolver(RequestContext request)
    {
        return null;
    }

    @Override
    protected WorkspaceManager getWorkspaceManager(RequestContext request)
    {
        return null;
    }
}
