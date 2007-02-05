/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.osgi;

import org.mule.config.ConfigurationException;
import org.mule.config.i18n.Message;
import org.mule.providers.service.TransportServiceDescriptor;
import org.mule.registry.ServiceDescriptor;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.util.SpiUtils;
import org.mule.util.StringUtils;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;


public class TransportActivator implements BundleActivator {
    
    public static final String OSGI_HEADER_TRANSPORTS = "Mule-Transports";
    
    public void start(BundleContext bc) throws Exception {
        Dictionary headers = bc.getBundle().getHeaders();
        
        // The transport(s) should have been declared as a manifest header, e.g.:
        //   Mule-Transports: http, https, servlet
        String transportHeader = (String) headers.get(OSGI_HEADER_TRANSPORTS);
        if (transportHeader == null)
        {
            throw new ConfigurationException(Message.createStaticMessage("Transport must declare its protocol as an OSGi header."));
        }
        String[] transports = StringUtils.splitAndTrim(transportHeader, ",");

        String transport;
        for (int i=0; i<transports.length; ++i)
        {
            transport = transports[i];
            // Look up the service descriptor file (e.g., "tcp.properties")
            String descriptorPath = "/" + SpiUtils.SERVICE_ROOT + SpiUtils.PROVIDER_SERVICE_PATH + transport + ".properties";
            URL descriptorUrl = bc.getBundle().getEntry(descriptorPath);
            if (descriptorUrl == null)
            {
                throw new ConfigurationException(Message.createStaticMessage("Unable to locate service descriptor file: " + descriptorPath));
            }
            Properties props = new Properties();
            props.load(descriptorUrl.openStream());
            ServiceDescriptor descriptor = 
                ServiceDescriptorFactory.create(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, transport, props, null);
    
            // Register the ServiceDescriptor as an OSGi Service.
            Hashtable osgiProps = new Hashtable();
            osgiProps.put(Constants.SERVICE_PID, headers.get(Constants.BUNDLE_SYMBOLICNAME) + "." + transport);
            osgiProps.put(Constants.SERVICE_DESCRIPTION, headers.get(Constants.BUNDLE_DESCRIPTION));
            osgiProps.put(Constants.SERVICE_VENDOR, headers.get(Constants.BUNDLE_VENDOR));
            bc.registerService(TransportServiceDescriptor.class.getName(), descriptor, osgiProps);    
        }
    }

    public void stop(BundleContext bc) throws Exception {
    }
}
