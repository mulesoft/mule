/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.xni.parser;

import static org.mule.runtime.config.internal.util.SchemaMappingsUtils.CUSTOM_SCHEMA_MAPPINGS_LOCATION;
import static org.mule.runtime.config.internal.util.SchemaMappingsUtils.getSchemaMappings;
import static org.mule.runtime.config.internal.util.SchemaMappingsUtils.resolveSystemId;
import static org.slf4j.LoggerFactory.getLogger;

import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Custom {@link XMLEntityResolver} that resolve entities over mule schemas.
 *
 * @since 4.4.0
 */
public class DefaultXmlEntityResolver implements XMLEntityResolver {

  private static final Logger LOGGER = getLogger(DefaultXmlEntityResolver.class);

  private final Map<String, String> schemas;

  public DefaultXmlEntityResolver() {
    this.schemas = getSchemaMappings(CUSTOM_SCHEMA_MAPPINGS_LOCATION, DefaultXmlEntityResolver.class::getClassLoader);
  }

  @Override
  public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier) throws XNIException, IOException {
    String publicId = resourceIdentifier.getPublicId();
    String systemId = resourceIdentifier.getExpandedSystemId();
    if (publicId == null && systemId == null)
      return null;
    systemId = resolveSystemId(publicId, systemId, (pId, sId) -> schemas.containsKey(pId));
    return resolveEntity(publicId, systemId);
  }

  private XMLInputSource resolveEntity(String publicId, String systemId) {
    String resourceLocation = schemas.get(systemId);
    if (resourceLocation != null) {
      InputStream is = DefaultXmlEntityResolver.class.getClassLoader().getResourceAsStream(resourceLocation);
      if (is == null) {
        LOGGER.debug("Couldn't find XML schema [" + systemId + "]: " + resourceLocation);
        return null;
      }
      XMLInputSource source = new XMLInputSource(publicId, systemId, null);
      source.setByteStream(is);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Found XML schema [" + systemId + "] in classpath: " + resourceLocation);
      }
      return source;
    } else {
      return null;
    }
  }
}
