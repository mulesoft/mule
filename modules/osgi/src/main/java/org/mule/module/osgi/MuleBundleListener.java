/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.osgi;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.config.ConfigResource;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.util.StringUtils;

import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.util.tracker.ServiceTracker;

public class MuleBundleListener implements BundleActivator
{
    /** Manifest entry name for listing Mule config files */
    public static final String MULE_CONFIG_HEADER = "Mule-Config";
    
    private BundleContext context;
    
    /** The id of the listener bundle itself */
    protected long bundleId;
    
    /** Bundle listener interested in Mule configs */
    private SynchronousBundleListener muleConfigListener;
    
    /** Reference to the MuleContext */
    ServiceTracker muleContextRef;
    
    private static Log logger = LogFactory.getLog(MuleBundleListener.class);
    
    private class MuleConfigListener implements SynchronousBundleListener
    {
        public void bundleChanged(BundleEvent event)
        {
            Bundle bundle = event.getBundle();

            // ignore current bundle 
            if (bundle.getBundleId() == bundleId)
            {
                return;
            }

            switch (event.getType())
            {
                case BundleEvent.INSTALLED :
                {
                    Enumeration schemas = bundle.findEntries("/", "*.xsd", true);
                    while (schemas.hasMoreElements())
                    {
                        URL url = (URL) schemas.nextElement();
                        logger.debug("Schema found in bundle: " + url);
                    }
                }
                case BundleEvent.STARTING :
                {
                    String configResourceList = getMuleConfigResources(bundle);
                    if (configResourceList != null)
                    {
                        logger.info("Processing Mule configuration: " + configResourceList);
                        MuleContext muleContext = (MuleContext) muleContextRef.getService();
                        
                        // TODO This should be run in a separate non-blocking thread.
                        try
                        {
                            String[] configResources = StringUtils.splitAndTrim(configResourceList, ",; ");                            
                            ConfigResource[] configResourceUrls = new ConfigResource[configResources.length];
                            for (int i = 0; i < configResources.length; i++)
                            {
                                // Create a URL to the resource within the bundle jar (Resource files cannot be 
                                // directly accessed across bundles through the classloader, unfortunately).
                                configResourceUrls[i] = new ConfigResource(bundle.getEntry(configResources[i]));
                            }
                            
                            SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder(configResourceUrls);
                            builder.setUseDefaultConfigResource(false);
                            builder.configure(muleContext);
                        }
                        catch (ConfigurationException ce)
                        {
                            logger.error(ce);
                            
                            //logger.info("Unable to process Mule configuration, stopping bundle...");
                            // TODO How to abort bundle startup?  The following throws
                            //      "BundleException: Bundle.start called from BundleActivator.stop"
                            // bundle.stop();
                        }
                    }
                    break;
                }
                case BundleEvent.STOPPING :
                {
                    // TODO Tear down Mule config somehow
                    break;
                }
                default :
                    break;
            }
        }
    }

    public void start(BundleContext context) throws Exception
    {
        this.context = context;
        this.bundleId = context.getBundle().getBundleId();
        
        muleContextRef = new ServiceTracker(context, MuleContext.class.getName(), null);
        muleContextRef.open();

        muleConfigListener = new MuleConfigListener();
        // listen to any changes in bundles
//        context.addBundleListener(muleConfigListener);
    }

    public void stop(BundleContext context) throws Exception
    {
        if (muleConfigListener != null)
        {
            context.removeBundleListener(muleConfigListener);
            muleConfigListener = null;
        }
    }

    protected String getMuleConfigResources(Bundle bundle)
    {
        Dictionary headers = bundle.getHeaders();
        Object header = null;
        if (headers != null)
        {
            header = headers.get(MULE_CONFIG_HEADER);
        }
        return (header != null ? header.toString() : null);
    }
}
