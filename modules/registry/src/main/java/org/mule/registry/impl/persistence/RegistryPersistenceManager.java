/*
 * $Id: $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.impl.persistence;

import org.mule.persistence.Persistable;
import org.mule.registry.impl.store.PersistenceManager;
import org.mule.transformers.xml.XStreamFactory;

import com.thoughtworks.xstream.XStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * PLACEHOLDER
 *
 * @author 
 * @version $Revision: $
 */
public class RegistryPersistenceManager extends PersistenceManager
{
    private XStreamFactory xstreamFactory = null;

    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(RegistryPersistenceManager.class);

    public RegistryPersistenceManager()
    {
        try 
        {
            xstreamFactory = new XStreamFactory();
        } catch (Exception e)
        {
            logger.error("Unable to initialize the XStreamFactory: " + 
                    e.toString());
            xstreamFactory = null;
        }
    }

    public void requestPersistence(Persistable source) 
    {
        logger.info("Got request to persist");

        try {
            Object data = source.getPersistableObject();
            XStream xstream = xstreamFactory.getInstance();
            //logger.info(xstream.toXML(data));
        } catch (Exception e)
        {
        }
    }

}


