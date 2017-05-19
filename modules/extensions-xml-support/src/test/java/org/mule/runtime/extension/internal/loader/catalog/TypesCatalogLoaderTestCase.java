/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.catalog;

import org.junit.Assert;
import org.junit.Test;
import org.mule.runtime.extension.internal.loader.catalog.loader.xml.TypesCatalogXmlLoader;
import org.mule.runtime.extension.internal.loader.catalog.model.TypesCatalog;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;

/**
 * TODO(fernandezlautaro: MULE-11501 this class must be moved to a separate module
 */
public class TypesCatalogLoaderTestCase extends AbstractMuleTestCase {

  //TODO(fernandezlautaro: MULE-11501 resources under the following folder must be moved too
  private static final File TESTS_FOLDER = new File("src/test/resources/catalog");

  @Test
  public void typesJsonCatalogCanBeLoadedFromXmlFile() throws Exception {
    final File appTypesFile = new File(TESTS_FOLDER, "json-catalog.xml");
    TypesCatalogXmlLoader typesCatalogXmlLoader = new TypesCatalogXmlLoader();
    final TypesCatalog typesCatalog = typesCatalogXmlLoader.load(appTypesFile.toURI().toURL());
    Assert.assertTrue(typesCatalog.resolveType("JsonType1").isPresent());
    Assert.assertTrue(typesCatalog.resolveType("JsonType2").isPresent());
  }

  @Test
  public void typesXsdCatalogCanBeLoadedFromXmlFile() throws Exception {
    final File appTypesFile = new File(TESTS_FOLDER, "xsd-catalog.xml");
    TypesCatalogXmlLoader typesCatalogXmlLoader = new TypesCatalogXmlLoader();
    final TypesCatalog typesCatalog = typesCatalogXmlLoader.load(appTypesFile.toURI().toURL());
    Assert.assertTrue(typesCatalog.resolveType("XsdType1").isPresent());
    Assert.assertTrue(typesCatalog.resolveType("XsdType2").isPresent());
    Assert.assertTrue(typesCatalog.resolveType("XsdType3").isPresent());
  }

  @Test
  public void typesJsonAndXsdCatalogCanBeLoadedFromXmlFile() throws Exception {
    final File appTypesFile = new File(TESTS_FOLDER, "json-and-xsd-catalog.xml");
    TypesCatalogXmlLoader typesCatalogXmlLoader = new TypesCatalogXmlLoader();
    final TypesCatalog typesCatalog = typesCatalogXmlLoader.load(appTypesFile.toURI().toURL());
    Assert.assertTrue(typesCatalog.resolveType("XsdType1").isPresent());
    Assert.assertTrue(typesCatalog.resolveType("XsdType2").isPresent());
    Assert.assertTrue(typesCatalog.resolveType("JsonType1").isPresent());
    Assert.assertTrue(typesCatalog.resolveType("JsonType2").isPresent());
  }

}
