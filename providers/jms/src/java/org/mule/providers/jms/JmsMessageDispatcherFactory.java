/*
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 24-Feb-2004
 * Time: 22:06:00
 */
package org.mule.providers.jms;

import org.mule.umo.UMOException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.provider.UMOMessageDispatcherFactory;

public class JmsMessageDispatcherFactory implements UMOMessageDispatcherFactory
{
    public UMOMessageDispatcher create(UMOConnector connector) throws UMOException
    {
        return new JmsMessageDispatcher((JmsConnector) connector);
    }
}