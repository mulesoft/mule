/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.registry;


import java.util.Properties;


/**
 * <code>ServiceFinder</code> can be used as a hook into the service lookup process to 
 * return the correct Service Descriptor for a given service name. By default a service 
 * is looked up directly, however a generic service name might be used where the real 
 * service implementation will depend on other modules/resources being available.
 * For example, in the case of a SOAP connector the finder could check the classpath for 
 * Axis or CXF and return the correct descriptor.
 */
public interface ServiceFinder
{
    String findService(String service, ServiceDescriptor descriptor, Properties props) throws ServiceException;
}


