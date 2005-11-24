/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.soap.axis.extensions;

import org.apache.axis.ConfigurationException;
import org.apache.axis.configuration.FileProvider;
import org.apache.axis.handlers.soap.SOAPService;

import javax.xml.namespace.QName;
import java.io.InputStream;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class WSDDFileProvider extends FileProvider {
    /**
     * Constructor which accesses a file in the current directory of the
     * engine or at an absolute path.
     */
    public WSDDFileProvider(String filename) {
        super(filename);
    }

    /**
     * Constructor which accesses a file relative to a specific base
     * path.
     */
    public WSDDFileProvider(String basepath, String filename) throws ConfigurationException {
        super(basepath, filename);
    }

    /**
     * retrieve an instance of the named service
     *
     * @param qname XXX
     * @return XXX
     * @throws org.apache.axis.ConfigurationException
     *          XXX
     */
    public SOAPService getService(QName qname) throws ConfigurationException {
        return getDeployment().getService(qname);
    }

    /**
     * Constructor which takes an input stream directly.
     * Note: The configuration will be read-only in this case!
     */
    public WSDDFileProvider(InputStream is) {
        super(is);
    }
}
