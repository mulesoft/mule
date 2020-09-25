/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.xni.parser;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.config.internal.util.SchemaMappingsUtils.CUSTOM_SCHEMA_MAPPINGS_LOCATION;
import static org.mule.runtime.config.internal.util.SchemaMappingsUtils.getSchemaMappings;
import static org.slf4j.LoggerFactory.getLogger;

import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import org.mule.runtime.config.api.xni.parser.XmlSchemaProvider;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link XmlSchemaProvider}
 *
 * @since 4.4.0
 */
public class DefaultXmlSchemaProvider implements XmlSchemaProvider {

  private static final Logger LOGGER = getLogger(XmlSchemaProvider.class);

  private final Map<String, String> schemas;

  public DefaultXmlSchemaProvider() {
    this.schemas = getSchemaMappings(CUSTOM_SCHEMA_MAPPINGS_LOCATION, DefaultXmlSchemaProvider.class::getClassLoader);
  }

  public Map<String, String> getSchemasMappings() {
    return schemas;
  }

  @Override
  public List<XMLInputSource> getSchemas() {
    return schemas.entrySet().stream()
        .map(entry -> {
          String systemId = entry.getKey();
          String resourceLocation = entry.getValue();
          XMLInputSource xis = null;
          InputStream is = DefaultXmlSchemaProvider.class.getClassLoader().getResourceAsStream(resourceLocation);
          if (is == null) {
            LOGGER.debug("Couldn't find XML schema [" + systemId + "]: " + resourceLocation);
          } else {
            xis = new XMLInputSource(null, entry.getKey(), null);
            xis.setByteStream(is);
          }
          return ofNullable(xis);
        })
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }
}
