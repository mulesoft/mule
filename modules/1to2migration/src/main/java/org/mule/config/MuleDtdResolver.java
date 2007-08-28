/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * <code>MuleDtdResolver</code> attempts to locate the mule-configuration.dtd on
 * the classpath, regardless of the DOCTYPE declaration. If the DTD is not found, it
 * defaults to trying to download it using the systemId. <p/> This resolver is
 * responsible for associating an XSL document if any with the DTD. It also allows
 * for a delegate Entity resolver and delegate XSL. This allows Configuration
 * builders to mix Mule Xml configuration with other document based configuration and
 * apply transformers to each of the configuration types (if necessary) before
 * constucting a Mule instance. <p/> Note that its up to the Configuration builder
 * implementation to do the actual transformations this Resolver simply associates
 * XSL resources with DTDs.
 */
public class MuleDtdResolver implements EntityResolver
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MuleDtdResolver.class);

    public static final String DEFAULT_MULE_DTD = "mule-configuration.dtd";
//    private String dtdName = null;

    // Maybe the dtd should go in the META-INF??
    private static final String SEARCH_PATH = "";

    private EntityResolver delegate;
    private String xsl;
    private static String currentXsl;

    public MuleDtdResolver()
    {
        this(DEFAULT_MULE_DTD);
    }

    public MuleDtdResolver(String dtdName)
    {
        this(dtdName, null, null);
    }

    public MuleDtdResolver(String dtdName, String xsl)
    {
        this(dtdName, xsl, null);
    }

    public MuleDtdResolver(String dtdName, EntityResolver delegate)
    {
        this(dtdName, null, delegate);
    }

    public MuleDtdResolver(String dtdName, String xsl, EntityResolver delegate)
    {
//        this.dtdName = dtdName;
        this.delegate = delegate;
        this.xsl = xsl;
        if (logger.isDebugEnabled())
        {
            StringBuffer buffer = new StringBuffer();
            buffer.append("Created Mule Dtd Resolver: ");
            buffer.append("dtd=").append(dtdName).append(", ");
            buffer.append("xsl=").append(xsl).append(", ");
            buffer.append("delegate resolver=").append(delegate).append(", ");
            logger.debug(buffer.toString());
        }
    }

    public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException
    {
        logger.debug("Trying to resolve XML entity with public ID: " + publicId + " and system ID: "
                     + systemId);

        InputSource source = null;
        currentXsl = null;
        if (delegate != null)
        {
            source = delegate.resolveEntity(publicId, systemId);
        }
        if ((source == null) && StringUtils.isNotBlank(systemId) && systemId.endsWith(".dtd"))
        {
            String[] tokens = systemId.split("/");
            String dtdFile = tokens[tokens.length - 1];
            logger.debug("Looking on classpath for " + SEARCH_PATH + dtdFile);

            InputStream is = IOUtils.getResourceAsStream(SEARCH_PATH + dtdFile, getClass(), /* tryAsFile */
                true, /* tryAsUrl */false);
            if (is != null)
            {
                source = new InputSource(is);
                source.setPublicId(publicId);
                source.setSystemId(systemId);
                logger.debug("Found on classpath mule DTD: " + systemId);
                currentXsl = xsl;
                return source;
            }
            logger.debug("Could not find dtd resource on classpath: " + SEARCH_PATH + dtdFile);
        }
        return source;
    }

    public String getXslForDtd()
    {
        return currentXsl;
    }
}
