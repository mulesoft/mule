/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.extensions;

import javax.xml.namespace.QName;

import org.apache.axis.ConfigurationException;
import org.apache.axis.configuration.FileProvider;
import org.apache.axis.handlers.soap.SOAPService;

/**
 * Override the File provider to stop exceptions being thrown in Axis if the service
 * does not exist. Mule adds services after the WSDD has been loaded.
 */
public class WSDDFileProvider extends FileProvider
{
    /**
     * Constructor which accesses a file in the current directory of the engine or at
     * an absolute path.
     */
    public WSDDFileProvider(String filename)
    {
        super(filename);
    }

    /**
     * Constructor which accesses a file relative to a specific base path.
     */
    public WSDDFileProvider(String basepath, String filename) throws ConfigurationException
    {
        super(basepath, filename);
    }

    /**
     * retrieve an instance of the named service
     * 
     * @param qname the name of the service
     * @return the service object or null if it doesn't exist
     * @throws org.apache.axis.ConfigurationException
     */
    public SOAPService getService(QName qname) throws ConfigurationException
    {
        return getDeployment().getService(qname);
    }
}
