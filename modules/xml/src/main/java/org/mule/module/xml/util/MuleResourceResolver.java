/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.util;

import org.mule.util.IOUtils;
import org.mule.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Attempts to resolve a resource from the file system, from a URL, or from the
 * classpath, in that order.
 */
public class MuleResourceResolver implements LSResourceResolver
{
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseUri)
    {
        try
        {
            logger.debug("Resolving resource : " + systemId);
            return obtainInputStream(type, namespaceURI, publicId, systemId, baseUri);
        }
        catch (Exception e)
        {
            //  In case of error, suggest the parser to open the resource as URL
            // by returning a null value.
            logger.debug("Cannot resolve resource " + systemId + " with LocalResourceResolver");
            return null;
        }
    }

    private LSInput obtainInputStream(String type, String namespaceURI, String publicId, String systemId, String baseUri) throws URISyntaxException, IOException
    {
        String resource = resolveUri(systemId, baseUri);

        LocalResourceResolverInput input = new LocalResourceResolverInput();
        InputStream stream = IOUtils.getResourceAsStream(resource, getClass());
        if (stream == null)
        {
            return null;
        }

        input.setPublicId(publicId);
        input.setSystemId(systemId);
        input.setBaseURI(baseUri);
        input.setByteStream(stream);

        return input;
    }

    private String resolveUri(String systemId, String baseURI) throws URISyntaxException
    {
        String resource;

        if (StringUtils.isNotBlank(baseURI))
        {
            URI baseUriObject = new URI(baseURI);
            URI absoluteUri = baseUriObject.resolve(systemId);
            resource = absoluteUri.toASCIIString();
        }
        else
        {
            resource = systemId;
        }

        return resource;
    }
}
