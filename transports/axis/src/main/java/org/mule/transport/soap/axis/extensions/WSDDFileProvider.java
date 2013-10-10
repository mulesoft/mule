/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
