package org.mule.extras.oracle.jms;

import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.JmsMessageDispatcherFactory;
import org.mule.umo.UMOException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;

/**
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class OracleJmsMessageDispatcherFactory extends JmsMessageDispatcherFactory
{
    public UMOMessageDispatcher create(UMOConnector connector) throws UMOException {
        return new OracleJmsMessageDispatcher((JmsConnector) connector);
    }
}