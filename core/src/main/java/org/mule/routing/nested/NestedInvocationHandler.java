package org.mule.routing.nested;

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.UMONestedRouter;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NestedInvocationHandler implements InvocationHandler
{

    static Log logger = LogFactory.getLog(NestedInvocationHandler.class);

    public static final String DEFAULT_METHOD_NAME_TOKEN = "default";

    private Map routers = new ConcurrentHashMap();

    protected NestedInvocationHandler(UMONestedRouter router)
    {
        addRouterForInterface(router);
    }

    public void addRouterForInterface(UMONestedRouter router)
    {
        if(router.getMethod()==null)
        {
            if(routers.size()==0)
            {
                routers.put(DEFAULT_METHOD_NAME_TOKEN, router);
            }
            else
            {
                throw new IllegalArgumentException(new Message(Messages.MUST_SET_METHOD_NAMES_ON_BINDING).toString());
            }
        }
        else
        {
            routers.put(router.getMethod(), router);
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {

        UMOMessage message = new MuleMessage(args[0]);
        UMONestedRouter router = (UMONestedRouter)routers.get(method.getName());
        if(router==null)
        {
            router = (UMONestedRouter)routers.get(DEFAULT_METHOD_NAME_TOKEN);
        }

        if(router==null)
        {
            throw new IllegalArgumentException(new Message(Messages.CANNOT_FINDE_BINDING_FOR_METHOD_X, method.getName()).toString());
        }
        UMOEndpoint endpoint = router.getEndpoint();

        UMOMessage reply;

        UMOEvent currentEvent = RequestContext.getEvent();
        final MuleEvent event = new MuleEvent(message, endpoint, currentEvent.getComponent(), currentEvent);

        reply = router.route(event);

        if (reply != null)
        {
            return reply.getPayload();
        }
        else
        {
            return null;
        }
    }

}