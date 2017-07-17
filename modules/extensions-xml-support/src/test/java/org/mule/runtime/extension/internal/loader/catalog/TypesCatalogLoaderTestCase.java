/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.catalog;

import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.api.util.ClassUtils.getResource;

import org.junit.Test;
import org.mule.runtime.extension.internal.loader.catalog.loader.xml.TypesCatalogXmlLoader;
import org.mule.runtime.extension.internal.loader.catalog.model.TypesCatalog;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

/**
 * TODO(fernandezlautaro: MULE-11501 this class must be moved to a separate module
 */
public class TypesCatalogLoaderTestCase extends AbstractMuleTestCase {

  //TODO(fernandezlautaro: MULE-11501 resources under src/test/resources/catalog must be moved too

  @Test
  public void typesJsonCatalogCanBeLoadedFromXmlFile() throws Exception {
    final File appTypesFile = getResourceFile("json-catalog.xml");
    TypesCatalogXmlLoader typesCatalogXmlLoader = new TypesCatalogXmlLoader();
    final TypesCatalog typesCatalog = typesCatalogXmlLoader.load(appTypesFile.toURI().toURL());
    assertTrue(typesCatalog.resolveType("JsonType1").isPresent());
    assertTrue(typesCatalog.resolveType("JsonType2").isPresent());
  }

  @Test
  public void typesXsdCatalogCanBeLoadedFromXmlFile() throws Exception {
    final File appTypesFile = getResourceFile("xsd-catalog.xml");
    TypesCatalogXmlLoader typesCatalogXmlLoader = new TypesCatalogXmlLoader();
    final TypesCatalog typesCatalog = typesCatalogXmlLoader.load(appTypesFile.toURI().toURL());
    assertTrue(typesCatalog.resolveType("XsdType1").isPresent());
    assertTrue(typesCatalog.resolveType("XsdType2").isPresent());
    assertTrue(typesCatalog.resolveType("XsdType3").isPresent());
  }

  @Test
  public void typesJsonAndXsdCatalogCanBeLoadedFromXmlFile() throws Exception {
    final File appTypesFile = getResourceFile("json-and-xsd-catalog.xml");
    TypesCatalogXmlLoader typesCatalogXmlLoader = new TypesCatalogXmlLoader();
    final TypesCatalog typesCatalog = typesCatalogXmlLoader.load(appTypesFile.toURI().toURL());
    assertTrue(typesCatalog.resolveType("XsdType1").isPresent());
    assertTrue(typesCatalog.resolveType("XsdType2").isPresent());
    assertTrue(typesCatalog.resolveType("JsonType1").isPresent());
    assertTrue(typesCatalog.resolveType("JsonType2").isPresent());
  }

  private File getResourceFile(String filename) throws URISyntaxException {
    return new File(getResource(Paths.get("catalog", filename).toString(), getClass()).toURI());
  }

}
