package org.mule.module.extension.internal.runtime.connector.secure;

import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.ConnectionProvider;
import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.param.display.Password;

public class SecureConnectionProvider implements ConnectionProvider<SecureConnector, Object>
{

    @Parameter
    @Password
    private String providerPassword;

    @Override
    public Object connect(SecureConnector secureConnector) throws ConnectionException
    {
        return null;
    }

    @Override
    public void disconnect(Object o)
    {

    }

    @Override
    public ConnectionHandlingStrategy<Object> getHandlingStrategy(ConnectionHandlingStrategyFactory<SecureConnector, Object> connectionHandlingStrategyFactory)
    {
        return null;
    }
}
