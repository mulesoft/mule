
package org.mule.transport.sftp.notification;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.sftp.SftpConnector;

public class SftpNotifier
{

    private SftpConnector connector;
    private MuleMessage message;
    private ImmutableEndpoint endpoint;
    private String serviceName;

    public SftpNotifier(SftpConnector connector,
                        MuleMessage message,
                        ImmutableEndpoint endpoint,
                        String serviceName)
    {

        this.connector = connector;
        this.message = message;
        this.endpoint = endpoint;
        this.serviceName = serviceName;
    }

    public void setMessage(MuleMessage message)
    {
        this.message = message;
    }

    public void notify(int action, String info)
    {
        notify(action, info, -1);
    }

    public void notify(int action, String info, long size)
    {
        if (connector.isEnableMessageEvents())
        {
            connector.fireNotification(new SftpTransportNotification(message, endpoint, serviceName, action,
                info, size));
        }
    }
}
