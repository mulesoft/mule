package org.mule.umo.routing;

import org.mule.umo.MessagingException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;

public interface UMONestedRouter extends UMORouter
{

	UMOMessage route(UMOEvent event) throws MessagingException;

    void setEndpoint(UMOEndpoint endpoint);

    UMOEndpoint getEndpoint();

    Class getInterface();

    void setInterface(Class interfaceClass);

    String getMethod();

    void setMethod(String method);

    /**
	 * This wires the dynamic proxy to the service object.
	 *
	 * @param target
	 */
	Object createProxy(Object target);
}
