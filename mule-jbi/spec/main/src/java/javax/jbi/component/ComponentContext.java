// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * ComponentContext.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package javax.jbi.component;
 
import java.util.MissingResourceException;
import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;   

/**
 * This interface provides access to data needed by a JBI component about the
 * JBI environment in which it is installed, as well providing the means to
 * allow the component to inform the JBI environment about services provided by
 * this component. This interface provides methods for the following
 * functions:
 * <ul>
 *   <li>Get the <b>DeliveryChannel</b> for this component. This is required
 *       to allow the component to send and receive message exchanges.</li>
 *   <li><b>Activate</b> (and deactivate) service endpoints provided by this
 *       component.</li>
 *   <li><b>Register</b> (and deregister) external endpoints provided by this
 *       component.</li>
 *   <li><b>Query</b> available endpoints (internal and external).</li>
 *   <li><b>Query</b> various data about the component, as installed in
 *       the JBI environment (name, workspace root, install root, initial JNDI
 *       context, MBean Server, Transaction Manager).</li>
 *   <li><b>Loggers.</b> Obtain the component's logger and subloggers.</li>
 *   <li><b>MBean name creator.</b> Access a utility for creating custom MBean
 *         names.</li>
 *   <li><b>EPR Resolver.</b> Ask JBI to resolve an endpoint reference (EPR),
 *       converting it into a service endpoint.</li>
 * </ul>
 * Note: The term "NMR" (meaning Normalized Message Router) is used here to
 * refer to the messaging system of the JBI implementation. This term is used
 * as a synonym for the JBI implementation, and refers only to the logical
 * message routing functions of a JBI implementation. It is not meant to
 * require that JBI implementations literally have a subsystem named "NMR".
 *
 * @author JSR208 Expert Group
 */
public interface ComponentContext
{
    /**
     * Activates the named endpoint with the NMR. Activation indicates to
     * the NMR that this component is ready to process requests sent to the
     * named endpoint.
     * <p>
     * Note that the JBI implementation may call this component's {@link
     * Component#getServiceDescription(ServiceEndpoint)} method before
     * returning from this method call; the component's implementation must
     * be ready to supply service description metadata before the result of
     * this activation call (a ServiceEndpoint) is known.
     *
     * @param serviceName qualified name of the service the endpoint exposes;
     *        must be non-null.
     * @param endpointName the name of the endpoint to be activated; must be
     *        non-null and non-empty.
     * @return a reference to the activated endpoint; must be non-null.
     * @exception JBIException if the endpoint cannot be activated.
     */
    ServiceEndpoint activateEndpoint(QName serviceName, String endpointName)
        throws JBIException;
    
    /**
     * Deactivates the given endpoint with the NMR. Deactivation indicates
     * to the NMR that this component will no longer process requests sent to
     * the named endpoint.
     *
     * @param endpoint reference to the endpoint to be deactivated; must be
     *        non-null.
     * @exception JBIException if the endpoint cannot be deactivated.
     */
    void deactivateEndpoint(ServiceEndpoint endpoint)
        throws JBIException;

    /**
     * Registers the given external endpoint with the NMR.  This indicates
     * to the NMR that the given endpoint is used as a proxy for external
     * service consumers to access an internal service of the same service
     * name (but a different endpoint name).
     *
     * @param externalEndpoint the external endpoint to be registered, must be
     *        non-null.
     * @exception JBIException if an external endpoint with the same name is
     *            already registered, by this or another component.
     */
    void registerExternalEndpoint(ServiceEndpoint externalEndpoint)
        throws JBIException;

    /**
     * Deregisters the given external endpoint with the NMR.  This indicates
     * to the NMR that the given external endpoint can no longer be used as a
     * proxy for external service consumers to access an internal service of
     * the same service name.
     *
     * @param externalEndpoint the external endpoint to be deregistered; must
     *        be non-null.
     * @exception JBIException if the given external endpoint was not previously
     *            registered.
     */
    void deregisterExternalEndpoint(ServiceEndpoint externalEndpoint)
        throws JBIException;

    /**
     * Resolve the given endpoint reference into a service endpoint. This is
     * called by the component when it has an EPR that it wants to resolve
     * into a service endpoint.
     * <p>
     * Note that the service endpoint returned refers to a dynamic endpoint;
     * the endpoint will exist only as long as this component retains a
     * strong reference to the object returned by this method. The endpoint
     * may not be included in the list of "activated" endpoints.
     *
     * @param epr endpoint reference as an XML fragment; must be non-null.
     * @return the service endpoint corresponding to the given endpoint
     *         reference; <code>null</code> if the reference cannot be resolved.
     */
    ServiceEndpoint resolveEndpointReference(DocumentFragment epr);

    /**
     * Get the unique component name of this component, ass assigned by the
     * identification section of this component's installation descriptor.
     *
     * @return the component name; must be non-null and non-empty.
     */
    String getComponentName();

    /**
     * Get a channel for this component to use to communicate with the
     * Normalized Message Router. This channel must be used by the component
     * to send and receive message exchanges.
     *
     * @return the delivery channel for this component; must be non-null.
     * @exception MessagingException if a channel has already been opened,
     *            but not yet closed.
     */
    DeliveryChannel getDeliveryChannel()
        throws MessagingException;

    /**
     * Get the service endpoint for the named activated endpoint, if any.
     *
     * @param service qualified-name of the endpoint's service; must be
     *        non-null.
     * @param name name of the endpoint; must be non-null.
     * @return the named endpoint, or <code>null</code> if the named endpoint
     *         is not activated.
     */
    ServiceEndpoint getEndpoint(QName service, String name);

    /**
     * Retrieve the service description metadata for the specified endpoint.
     * <p>
     * Note that the result can use either the WSDL 1.1 or WSDL 2.0 description
     * language.
     *
     * @param endpoint endpoint reference; must be non-null.
     * @return metadata describing endpoint, or <code>null</code> if metadata
     *         is unavailable.
     * @exception JBIException invalid endpoint reference.
     */
    Document getEndpointDescriptor(ServiceEndpoint endpoint)
        throws JBIException;

    /**
     * Queries the NMR for active endpoints that implement the given
     * interface. This will return the endpoints for all services and endpoints
     * that implement the named interface (portType in WSDL 1.1). This
     * method does NOT include external endpoints (those registered using
     * {@link #registerExternalEndpoint(ServiceEndpoint)}.
     *
     * @param interfaceName qualified name of interface/portType that is
     *        implemented by the endpoint; if <code>null</code> then all
     *        activated endpoints in the JBI environment must be returned.
     * @return an array of available endpoints for the specified interface name;
     *         must be non-null; may be empty.
     */
    ServiceEndpoint[] getEndpoints(QName interfaceName);

    /**
     * Queries the NMR for active endpoints belonging to the given service. This
     * method does NOT include external endpoints (those registered using
     * {@link #registerExternalEndpoint(ServiceEndpoint)}.
     *
     * @param serviceName qualified name of the service that the endpoints
     *        are part of; must be non-null.
     * @return an array of available endpoints for the specified  service name;
     *         must be non-null; may be empty.
     */
    ServiceEndpoint[] getEndpointsForService(QName serviceName);

    /**
     * Queries the NMR for external endpoints that implement the given
     * interface name. This methods returns only registered external endpoints
     * (see {@link #registerExternalEndpoint(ServiceEndpoint)}.
     *
     * @param interfaceName qualified name of interface implemented by the
     *        endpoints; must be non-null.
     * @return an array of available external endpoints for the specified
     *         interface name; must be non-null; may be empty.
     */
    ServiceEndpoint[] getExternalEndpoints(QName interfaceName);

    /**
     * Queries the NMR for external endpoints that are part of the given
     * service.
     *
     * @param serviceName qualified name of service that contains the endpoints;
     *        must be non-null.
     * @return an array of available external endpoints for the specified
     *         service name; must be non-null; may be empty.
     */
    ServiceEndpoint[] getExternalEndpointsForService(QName serviceName);

    /**
     * Get the installation root directory path for this component.
     * <p>
     * This method MUST return the file path formatted for the underlying
     * platform.
     *
     * @return the installation root directory path, in platform-specific
     *         form; must be non-null and non-empty.
     */
    String getInstallRoot();

    /**
     * Get a logger instance from JBI. Loggers supplied by JBI are guaranteed
     * to have unique names such that they avoid name collisions with loggers
     * from other components created using this method. The suffix parameter
     * allows for the creation of subloggers as needed. The JBI specification
     * says nothing about the exact names to be used, only that they must be
     * unique across components and the JBI implementation itself.
     *
     * @param suffix for creating subloggers; use an empty string for the base
     *        component logger; must be non-null.
     * @param resourceBundleName name of <code>ResourceBundle</code> to be used
     *        for localizing messages for the logger. May be <code>null</code>
     *        if none of the messages require localization. The resource, if
     *        non-null, must be loadable using the component's class loader
     *        as the initiating loader.
     * @return a standard logger, named uniquely for this component (plus the
     *         given suffix, if applicable); must be non-null.
     * @exception MissingResourceException if the ResourceBundleName is non-null
     *            and no corresponding resource can be found.
     * @exception JBIException if the resourceBundleName has changed from
     *            a previous invocation by this component of this method with
     *            the same suffix.
     */
    Logger getLogger(String suffix, String resourceBundleName)
        throws MissingResourceException, JBIException;

    /**
     * Get a reference to the MBeanNames creator for use in creating custom
     * MBean names.
     *
     * @return reference to the MBeanNames creator; must be non-null.
     */
    javax.jbi.management.MBeanNames getMBeanNames();

    /**
     * Get the JMX MBean server used to register all MBeans in the JBI
     * environment.
     *
     * @return a reference to the MBean server; must be non-null.
     */
    javax.management.MBeanServer getMBeanServer();

    /**
     * Get the JNDI naming context for this component. This context is a
     * standard JNDI <code>InitialContext</code> but its content will vary
     * based on the environment in which the JBI implementation is running.
     *
     * @return the JNDI naming context; must be non-null.
     */ 
    javax.naming.InitialContext getNamingContext();

    /**
     * Get the TransactionManager for this implementation. The instance
     * returned is an implementation of the standard JTA interface. If none
     * is available, this method returns <code>null</code>.
     * <p>
     * The object returned by this method is untyped, to allow this interface
     * to be compiled in environments that do not support JTA. If not null, the
     * object returned must be of type
     * <code>javax.transaction.TransactionManager</code>.
     * <p>
     * This downcast is necessary because JBI is used in environments that
     * do not support JTA (i.e., J2SE). Explicit use of JTA types would cause
     * compilation failures in such environments.
     *
     * @return A TransactionManager instance, or <code>null</code> if none
     *         is available in the execution environment.
     */
    Object getTransactionManager();

    /**
     * Get the root directory path for this component's private workspace.
     * <p>
     * This method MUST return the file path formatted for the underlying
     * platform.
     * <p>
     * The returned value must indicate a valid file path that the component
     * may use to write files to, and read files from.
     *
     * @return the private workspace root path, in platform-specific form;
     *         must be non-null and non-empty.
     */
    String getWorkspaceRoot();

}
