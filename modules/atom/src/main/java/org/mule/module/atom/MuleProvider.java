/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
