// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * ServiceEndpoint.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

package javax.jbi.servicedesc;

import javax.xml.namespace.QName;

/**
 * Reference to an endpoint, used to refer to an endpoint as well as query
 * information about the endpoint. An endpoint is an addressable entity in the
 * JBI system, used for accessing the provider of a specific service.
 *
 * @author JSR208 Expert Group
 */
public interface ServiceEndpoint
{
    /**
     * Get a reference to this endpoint, using an endpoint reference vocabulary
     * that is known to the provider.
     * @param operationName the name of the operation to be performed by a
     * consumer of the generated endpoint reference. Set to <code>null</code>
     * if this is not applicable.
     * @return endpoint reference as an XML fragment; <code>null</code> if the
     * provider does not support such references.
     */
    org.w3c.dom.DocumentFragment getAsReference(QName operationName);

    /**
     * Returns the name of this endpoint.
     * @return the endpoint name.
     */
    String getEndpointName();

    /**
     * Get the qualified names of all the interfaces implemented by this
     * service endpoint.
     * @return array of all interfaces implemented by this service endpoint;
     * must be non-null and non-empty.
     */
    javax.xml.namespace.QName[] getInterfaces();

    /**
     *  Returns the service name of this endpoint.
     *  @return the qualified service name.
     */
    javax.xml.namespace.QName getServiceName();
}
