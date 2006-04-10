package org.mule.providers.rmi;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.ConnectException;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;

import javax.naming.Context;

import java.lang.reflect.Method;
import java.net.InetAddress;

/**
 * Code by (c) 2005 P.Oikari.
 *
 * @author <a href="mailto:tsuppari@yahoo.co.uk">P.Oikari</a>
 * @version $Revision$
 */

public class RmiMessageReceiver extends AbstractMessageReceiver
{
    protected RmiConnector connector;

    protected RmiAble remoteObject = null;

    private Context jndi = null;

    private String bindName = null;

    public RmiMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint) throws InitialisationException
    {
        super(connector, component, endpoint);

        this.connector = (RmiConnector) connector;
    }

    /**
     * Actual initialization. Attempts to rebind service object to Jndi Tree for discovery
     *
     * @param endpoint
     * @throws InitialisationException
     */
    private void initialize(UMOEndpoint endpoint) throws InitialisationException
    {
        logger.debug("Initializing with endpoint " + endpoint);

        String rmiPolicyPath = connector.getSecurityPolicy();
        // TODO what's up here?
        String serverCodebasePath = connector.getServerCodebase();

        System.setProperty("java.security.policy", rmiPolicyPath);

        UMOEndpointURI endpointUri = endpoint.getEndpointURI();

        int port = endpointUri.getPort();

        if (port < 1) {
            port = RmiConnector.DEFAULT_RMI_REGISTRY_PORT;
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(endpointUri.getHost());

            bindName = endpointUri.getPath();

            remoteObject = getRmiObject();

            Method theMethod = remoteObject.getClass().getMethod("setReceiver", new Class[]{RmiMessageReceiver.class});
            theMethod.invoke(remoteObject, new Object[]{this});

            jndi = connector.getJndiContext(inetAddress.getHostAddress() + ":" + port);

            jndi.rebind(bindName, remoteObject);
        }
        catch (Exception e) {
            throw new InitialisationException(e, this);
        }

        logger.debug("Initialized successfully");
    }

    /**
     * Initializes endpoint
     *
     * @throws ConnectException
     */
    public void doConnect() throws ConnectException
    {
        try {
            // Do not reinit if RMI is already bound to JNDI!!!
            // TODO Test how things work under heavy load!!!
            // Do we need threadlocals or so!?!?

            // TODO [aperepel] consider AtomicBooleans here
            // for 'initialised/initialising' status, etc.
            if (null == remoteObject) {
                initialize(getEndpoint());
            }
        }
        catch (Exception e) {
            throw new ConnectException(e, this);
        }
    }

    /**
     * Unbinds Rmi class from registry
     */
    public void doDisconnect()
    {
        logger.debug("Disconnecting...");

        try {
            jndi.unbind(bindName);
        }
        catch (Exception e) {
            logger.error(e);
        }

        logger.debug("Disconnected successfully.");
    }

    /**
     * Gets RmiAble objetc for registry to add in.
     *
     * @return java.rmi.Remote and RmiAble implementing class
     * @throws InitialisationException
     */
    private RmiAble getRmiObject() throws InitialisationException
    {
        String className = connector.getServiceClassName();

        if (null == className) {
            throw new InitialisationException(new org.mule.config.i18n.Message("rmi", RmiConnector.NO_RMI_SERVICECLASS_SET), this);
        }

        RmiAble remote = null;

        try {
            remote = (RmiAble) Class.forName(className).newInstance();
        }
        catch (Exception e) {
            throw new InitialisationException(new org.mule.config.i18n.Message("rmi", RmiConnector.RMI_SERVICECLASS_INVOCATION_FAILED), e);
        }

        return (remote);
    }

    /**
     * Routes message forward
     *
     * @param message
     * @return
     * @throws MessagingException
     * @throws UMOException
     */
    public Object routeMessage(Object message) throws MessagingException, UMOException
    {
        UMOMessageAdapter adapter = connector.getMessageAdapter(message);

        return (routeMessage(new MuleMessage(adapter)));
    }
}
