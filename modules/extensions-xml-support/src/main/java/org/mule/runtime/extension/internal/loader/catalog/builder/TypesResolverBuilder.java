/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.catalog.builder;

import static java.lang.String.format;
import org.mule.runtime.extension.internal.loader.catalog.model.resolver.SingleTypeResolver;
import org.mule.runtime.extension.internal.loader.catalog.model.resolver.TypeResolver;

import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Builder for {@link TypeResolver}.
 * TODO MULE-13214: this class could be removed once MULE-13214 is done
 *
 * @since 4.0
 */
public class TypesResolverBuilder {

  private static final String JAR = "jar";
  private final URI baseUri;
  private String name;
  private String location;
  private String element;

  public TypesResolverBuilder(URI baseUri) {
    this.baseUri = baseUri;
  }

  public void name(String name) {
    this.name = name;
  }

  public void location(String location) {
    this.location = location;
  }

  public void element(String element) {
    this.element = element;
  }

  public TypeResolver build() throws Exception {
    URI schemaURI = baseUri != null ? customResolve() : new URI(location);
    return new SingleTypeResolver(name, schemaURI.toURL(), element);
  }

  /**
   * If the current {@code baseUri} is a JAR file the resolution of the schema (under {@code location}) is trickier as it needs
   * to be recalculated from the entry name (see {@link JarURLConnection#getEntryName()}), forming a new {@link URI}.
   * <p/>
   * As sample, let's say we have:
   * <ol>
   *   <li>{@code baseUri}: "jar:file:/Users/lautaro/.m2/repository/org/mule/extensions/smart/smart-connector-using-custom-types/1.0.0-SNAPSHOT/smart-connector-using-custom-types-1.0.0-SNAPSHOT-mule-plugin.jar!/module-using-custom-types-catalog.xml"</li>
   *   <li>{@code location}: "./get-issues-return-schema.json"</li>
   * </ol>
   * <p/>
   * The expected {@link URI} will have the value of
   * "jar:file:/Users/lautaro/.m2/repository/org/mule/extensions/smart/smart-connector-using-custom-types/1.0.0-SNAPSHOT/smart-connector-using-custom-types-1.0.0-SNAPSHOT-mule-plugin.jar!/get-issues-return-schema.json",
   * making that resource available through the usual {@link URI}/{@link URL} methods.
   *
   * @return an URI targeting the schema for the current {@code location} and {@code baseUri}
   */
  private URI customResolve() {
    // TODO(fernandezlautaro: MULE-11501 this class must be moved to a separate module
    final URI result;
    if (baseUri.getScheme().equals(JAR)) {
      try {
        final URI fileCatalogBase = new URI(baseUri.getSchemeSpecificPart());
        final URI resolvedScheme = fileCatalogBase.resolve(location);
        result = new URI(JAR + ":" + resolvedScheme);
      } catch (URISyntaxException e) {
        throw new RuntimeException(format("Cannot generate a relative URI for the resource [%s] given the base URI path [%s]",
                                          location, baseUri.toString()));
      }
    } else {
      result = baseUri.resolve(location);
    }
    return result;
  }
}
