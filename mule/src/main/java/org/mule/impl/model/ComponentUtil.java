package org.mule.impl.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.container.ContainerKeyPair;
import org.mule.providers.AbstractConnector;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOAsynchronousComponent;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.model.ModelException;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * Reusable methods for working with UMOComponents.
 *
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class ComponentUtil {

    /**
     * Starts a Mule Component.
     */
    public static void start(UMOComponent component) throws UMOException {
        start(component, /*paused*/false);
    }

    /**
     * Starts a Mule Component.
     * @param paused - Start component in a "paused" state (messages are received but not processed).
     */
    public static void start(UMOComponent component, boolean paused) throws UMOException {

        // Create the receivers for the component but do not start them yet.
        registerListeners(component);

        // We connect the receivers _before_ starting the component because there may be
        // some initialization required for the component which needs to have them connected.
        // For example, the org.mule.providers.soap.glue.GlueMessageReceiver adds
        // InitialisationCallbacks within its doConnect() method (see MULE-804).
        connectListeners(component);

        // Start (and pause) the component.
        component.start();
        if (paused) {
            pause(component);
        }

        // We start the receivers _after_ starting the component because if a message
        // gets routed to the component before it is started,
        // org.mule.impl.model.AbstractComponent.dispatchEvent() will throw a
        // ComponentException with message COMPONENT_X_IS_STOPPED (see MULE-526).
        startListeners(component);
    }

    /**
     * Stops a Mule Component.
     */
    public static void stop(UMOComponent component) throws UMOException {
        unregisterListeners(component);
        component.stop();
    }

    /**
     * Pauses event processing for a single Mule Component. Unlike
     * stop(), a paused component will still consume messages from the
     * underlying transport, but those messages will be queued until the
     * component is resumed.
     *
     * @throws ComponentException if the component is not "pausable"
     */
    public static void pause(UMOComponent component) throws UMOException {
        // Is the component "pausable"?
        if (UMOAsynchronousComponent.class.isAssignableFrom(component.getClass())) {
            ((UMOAsynchronousComponent) component).pause();
        } else {
            throw new ComponentException(Message.createStaticMessage("Component " + component + " cannot be paused because it does not implement UMOAsynchronousComponent."), null, component);
        }
    }

    /**
     * Resumes a single Mule Component that has been paused. If the component is
     * not paused nothing is executed.
     *
     * @throws ComponentException if the component is not "pausable"
     */
    public static void resume(UMOComponent component) throws UMOException {
        // Is the component "pausable"?
        if (UMOAsynchronousComponent.class.isAssignableFrom(component.getClass())) {
            ((UMOAsynchronousComponent) component).resume();
        } else {
            throw new ComponentException(Message.createStaticMessage("Component " + component + " cannot be resumed because it does not implement UMOAsynchronousComponent."), null, component);
        }
    }

    public static void registerListeners(UMOComponent component) throws UMOException {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints(component);

        for (Iterator it = endpoints.iterator(); it.hasNext();) {
            endpoint = (UMOEndpoint) it.next();
            try {
                endpoint.getConnector().registerListener(component, endpoint);
            } catch (UMOException e) {
                throw e;
            } catch (Exception e) {
                throw new ModelException(new Message(Messages.FAILED_TO_REGISTER_X_ON_ENDPOINT_X,
                                                     component.getDescriptor().getName(),
                                                     endpoint.getEndpointURI()), e);
            }
        }
    }

    public static void unregisterListeners(UMOComponent component) throws UMOException {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints(component);

        for (Iterator it = endpoints.iterator(); it.hasNext();) {
            endpoint = (UMOEndpoint) it.next();
            try {
                endpoint.getConnector().unregisterListener(component, endpoint);
            } catch (UMOException e) {
                throw e;
            } catch (Exception e) {
                throw new ModelException(new Message(Messages.FAILED_TO_UNREGISTER_X_ON_ENDPOINT_X,
                                                     component.getDescriptor().getName(),
                                                     endpoint.getEndpointURI()), e);
            }
        }
    }

    public static void startListeners(UMOComponent component) throws UMOException {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints(component);

        for (Iterator it = endpoints.iterator(); it.hasNext();) {
            endpoint = (UMOEndpoint) it.next();
            UMOMessageReceiver receiver = ((AbstractConnector) endpoint.getConnector()).getReceiver(component, endpoint);
            if (receiver != null
                && endpoint.getConnector().isStarted()
                && endpoint.getInitialState().equals(UMOEndpoint.INITIAL_STATE_STARTED)) {
                receiver.start();
            }
        }
    }

    public static void stopListeners(UMOComponent component) throws UMOException {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints(component);

        for (Iterator it = endpoints.iterator(); it.hasNext();) {
            endpoint = (UMOEndpoint) it.next();
            UMOMessageReceiver receiver = ((AbstractConnector) endpoint.getConnector()).getReceiver(component, endpoint);
            if (receiver != null) {
                receiver.stop();
            }
        }
    }

    public static void connectListeners(UMOComponent component) throws UMOException {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints(component);

        for (Iterator it = endpoints.iterator(); it.hasNext();) {
            endpoint = (UMOEndpoint) it.next();
            UMOMessageReceiver receiver = ((AbstractConnector) endpoint.getConnector()).getReceiver(component, endpoint);
            if (receiver != null) {
                try {
                    receiver.connect();
                } catch (Exception e) {
                    throw new ModelException(Message.createStaticMessage("Failed to connect listener for endpoint " + endpoint.getName()), e);
                }
            }
        }
    }

    public static void disconnectListeners(UMOComponent component) throws UMOException {
        UMOEndpoint endpoint;
        List endpoints = getIncomingEndpoints(component);

        for (Iterator it = endpoints.iterator(); it.hasNext();) {
            endpoint = (UMOEndpoint) it.next();
            UMOMessageReceiver receiver = ((AbstractConnector) endpoint.getConnector()).getReceiver(component, endpoint);
            if (receiver != null) {
                try {
                    receiver.disconnect();
                } catch (Exception e) {
                    throw new ModelException(Message.createStaticMessage("Failed to connect listener for endpoint " + endpoint.getName()), e);
                }
            }
        }
    }

    /**
     * Returns a list of all incoming endpoints on a component.
     */
    public static List getIncomingEndpoints(UMOComponent component) {
        List endpoints = new ArrayList();

        // Add inbound endpoints
        endpoints.addAll(component.getDescriptor().getInboundRouter().getEndpoints());
        // Add the (deprecated) single inbound endpoint.
        if (component.getDescriptor().getInboundEndpoint() != null) {
            endpoints.add(component.getDescriptor().getInboundEndpoint());
        }

        // Add response endpoints
        if (component.getDescriptor().getResponseRouter() != null
                && component.getDescriptor().getResponseRouter().getEndpoints() != null) {
            endpoints.addAll(component.getDescriptor().getResponseRouter().getEndpoints());
        }
        return endpoints;
    }

    /**
     * Creates a component based on its descriptor.
     */
    public static Object createComponent(MuleDescriptor descriptor) throws UMOException {
        UMOManager manager = MuleManager.getInstance();
        Object impl = descriptor.getImplementation();
        Object component = null;

        if(impl instanceof String) {
            impl = new ContainerKeyPair(null, impl);
        }
        if (impl instanceof ContainerKeyPair) {
            component = manager.getContainerContext().getComponent(impl);

            if(descriptor.isSingleton()) {
                descriptor.setImplementation(component);
            }
        } else {
            component = impl;
        }

        try {
            BeanUtils.populate(component, descriptor.getProperties());
        } catch (Exception e) {
            throw new InitialisationException(new Message(Messages.FAILED_TO_SET_PROPERTIES_ON_X, "Component '"
                    + descriptor.getName() + "'"), e, descriptor);
        }

        // Call any custom initialisers
        descriptor.fireInitialisationCallbacks(component);

        return component;
    }
}
