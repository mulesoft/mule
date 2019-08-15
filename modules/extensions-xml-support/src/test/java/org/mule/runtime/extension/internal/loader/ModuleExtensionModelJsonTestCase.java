/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader;

import static java.io.File.separator;
import static java.lang.Boolean.getBoolean;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.VERSION;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skyscreamer.jsonassert.JSONAssert;

import com.google.common.collect.ImmutableSet;

/**
 * Tests to ensure XSD generation coming from an XML using the {@link ExtensionModel} mechanism.
 *
 * @since 4.0
 */
@RunWith(Parameterized.class)
public class ModuleExtensionModelJsonTestCase extends AbstractMuleTestCase {

  private static final boolean UPDATE_EXPECTED_FILES_ON_ERROR =
      getBoolean(SYSTEM_PROPERTY_PREFIX + "extensionModelJson.updateExpectedFilesOnError");

  @Parameterized.Parameter
  public ExtensionModel extensionModel;

  @Parameterized.Parameter(1)
  public String expectedSource;

  @Parameterized.Parameter(2)
  public String extensionName;

  private ExtensionModelJsonSerializer jsonSerializer;
  private String expectedJson;

  @Parameterized.Parameters(name = "{2}.json")
  public static Collection<Object[]> data() {
    final List<String> extensions = new ArrayList<String>() {

      {
        add("module-calling-operations-within-module");
        add("module-capitalized-name");
        add("module-documentation");
        add("module-global-element");
        add("module-global-element-default");
        add("module-json-custom-types");
        add("module-properties");
        add("module-stereotypes");
        add("module-xsd-custom-types");
      }
    };

    Function<String, Object[]> stringFunction = moduleName -> {
      String moduleModelPath = "modules" + separator + "model" + separator + moduleName + ".json";
      String modulePath = "modules/" + moduleName + ".xml";

      ClassLoader contextClassLoader = currentThread().getContextClassLoader();
      Map<String, Object> parameters = new HashMap<>();
      parameters.put(RESOURCE_XML, modulePath);
      ExtensionModel extensionModel =
          new XmlExtensionModelLoader().loadExtensionModel(contextClassLoader, getDefault(getDependencyExtensions()), parameters);

      return new Object[] {extensionModel, moduleModelPath, extensionModel.getName()};
    };
    return extensions.stream().map(stringFunction).collect(toList());
  }

  @Before
  public void setUp() throws IOException {
    jsonSerializer = new ExtensionModelJsonSerializer(true);
    expectedJson = getResourceAsString(expectedSource, ModuleExtensionModelJsonTestCase.class).trim();
  }

  /**
   * Utility to batch fix input files when severe model changes are introduced.
   * Use carefully, not a mechanism to get away with anything.
   * First check why the generated json is different and make sure you're not introducing any bugs.
   * This should NEVER be committed as true
   *
   * @return whether or not the "expected" test files should be updated when comparison fails
   */
  private boolean shouldUpdateExpectedFilesOnError() {
    return UPDATE_EXPECTED_FILES_ON_ERROR;
  }

  @Test
  public void generate() throws Exception {
    final String json = jsonSerializer.serialize(extensionModel).trim();
    try {
      JSONAssert.assertEquals(expectedJson, json, true);
    } catch (AssertionError e) {

      if (shouldUpdateExpectedFilesOnError()) {
        updateExpectedJson(json);
      } else {
        System.out.println(json);

        throw e;
      }
    }
  }

  @Test
  public void load() {
    ExtensionModel result = jsonSerializer.deserialize(expectedJson);
    assertThat(result, is(extensionModel));
  }

  private void updateExpectedJson(String json) throws URISyntaxException, IOException {
    File root = new File(getResourceAsUrl(expectedSource, getClass()).toURI()).getParentFile()
        .getParentFile().getParentFile().getParentFile().getParentFile();
    File testDir = new File(root, "src/test/resources/");
    File target = new File(testDir, expectedSource);
    stringToFile(target.getAbsolutePath(), json);

    System.out.println(target.getAbsolutePath() + " was fixed");
  }

  private static Set<ExtensionModel> getDependencyExtensions() {
    ExtensionModel petstore = loadExtension(PetStoreConnector.class, emptySet());
    ExtensionModel marvel = loadExtension(MarvelExtension.class, emptySet());
    ExtensionModel ce = MuleExtensionModelProvider.getExtensionModel();
    return ImmutableSet.<ExtensionModel>builder().add(petstore).add(marvel).add(ce).build();
  }

  private static ExtensionModel loadExtension(Class extension, Set<ExtensionModel> deps) {
    Map<String, Object> ctx = new HashMap<>();
    ctx.put(TYPE_PROPERTY_NAME, extension.getName());
    ctx.put(VERSION, "1.0.0-SNAPSHOT");
    // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
    ctx.put("COMPILATION_MODE", true);
    return new DefaultJavaExtensionModelLoader()
        .loadExtensionModel(currentThread().getContextClassLoader(), DslResolvingContext.getDefault(deps), ctx);
  }
}
