/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static java.time.Instant.ofEpochMilli;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.api.util.IOUtils.toByteArray;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.runtime.module.extension.internal.resources.BaseExtensionResourcesGeneratorAnnotationProcessor.COMPILATION_MODE;
import static org.mule.test.oauth.ConnectionType.DUO;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.internal.event.NullEventFactory;
import org.mule.runtime.extension.api.component.ComponentParameterization;
import org.mule.runtime.extension.api.component.value.ValueDeclarer;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.oauth.ConnectionProperties;
import org.mule.test.oauth.ConnectionType;
import org.mule.test.oauth.TestOAuthExtension;
import org.mule.test.values.extension.MyPojo;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;

public class ResolverSetUtilsTestCase extends AbstractMuleContextTestCase {

  private static final String DEFAULT_PARAMETER_GROUP_NAME = "General";
  private static final String POJO_PARAMETER_NAME = "connectionProperties";

  private static final String POJO_CONNECTION_DESCRIPTION_FIELD_NAME = "connectionDescription";
  private static final String POJO_CONNECTION_TYPE_FIELD_NAME = "connectionType";
  private static final String POJO_CONNECTION_PROPERTY_GRADE_FIELD_NAME = "connectionPropertyGrade";
  private static final String POJO_CONNECTION_TIME_FIELD_NAME = "connectionTime";
  private static final String POJO_CONNECTION_IMPORTED_POJO_FIELD_NAME = "importedPojo";

  private static final String IMPORTED_POJO_ID_FIELD_NAME = "pojoId";
  private static final String IMPORTED_POJO_NAME_FIELD_NAME = "pojoName";
  private static final String IMPORTED_POJO_NUMBER_FIELD_NAME = "pojoNumber";
  private static final String IMPORTED_POJO_BOOLEAN_FIELD_NAME = "pojoBoolean";

  private static final String IMPORTED_POJO_ID_FIELD_VALUE = "pojoIdValue";
  private static final String IMPORTED_POJO_NAME_FIELD_VALUE = "pojoNameValue";
  private static final Integer IMPORTED_POJO_NUMBER_FIELD_VALUE = 1234;
  private static final boolean IMPORTED_POJO_BOOLEAN_FIELD_VALUE = false;

  private static final String POJO_CONNECTION_DESCRIPTION_FIELD_VALUE = "connectionDescriptionValue";
  private static final ConnectionType POJO_CONNECTION_TYPE_FIELD_VALUE = DUO;
  private static final String POJO_CONNECTION_PROPERTY_GRADE_FIELD_VALUE = "connectionPropertyGradeLiteralString";
  private static final ZonedDateTime POJO_CONNECTION_TIME_FIELD_VALUE =
      ZonedDateTime.ofInstant(ofEpochMilli(1619535600000l), ZoneId.of("-03:00"));
  private static final String POJO_CONNECTION_TIME_FIELD_VALUE_AS_STRING = "2021-04-27T12:00:00-03:00";
  private static final MyPojo POJO_CONNECTION_IMPORTED_POJO_FIELD_VALUE =
      new MyPojo(IMPORTED_POJO_ID_FIELD_VALUE, IMPORTED_POJO_NAME_FIELD_VALUE, IMPORTED_POJO_NUMBER_FIELD_VALUE,
                 IMPORTED_POJO_BOOLEAN_FIELD_VALUE);

  private static final ConnectionProperties POJO_PARAMETER_VALUE =
      new ConnectionProperties(POJO_CONNECTION_DESCRIPTION_FIELD_VALUE, POJO_CONNECTION_TYPE_FIELD_VALUE,
                               new Literal<String>() {

                                 @Override
                                 public Optional<String> getLiteralValue() {
                                   return of(POJO_CONNECTION_PROPERTY_GRADE_FIELD_VALUE);
                                 }

                                 @Override
                                 public Class<String> getType() {
                                   return null;
                                 }
                               },
                               POJO_CONNECTION_TIME_FIELD_VALUE, POJO_CONNECTION_IMPORTED_POJO_FIELD_VALUE);

  private ReflectionCache reflectionCache = new ReflectionCache();

  private ExpressionManager expressionManager;

  private ExtensionModel testOAuthExtensionModel;
  private ParameterizedModel testParameterizedModel;

  @Before
  public void setup() throws IOException {
    testOAuthExtensionModel = loadExtension(TestOAuthExtension.class, new DefaultJavaExtensionModelLoader());

    expressionManager = muleContext.getExpressionManager();
    testParameterizedModel = testOAuthExtensionModel.getConfigurationModels().get(0).getConnectionProviders().get(0);
  }

  @Test
  public void complexParameter() throws Exception {
    Consumer<ValueDeclarer> valueDeclarerConsumer = valueDeclarer -> valueDeclarer.asObjectValue()
        .withField(POJO_CONNECTION_DESCRIPTION_FIELD_NAME, POJO_CONNECTION_DESCRIPTION_FIELD_VALUE)
        .withField(POJO_CONNECTION_TYPE_FIELD_NAME, POJO_CONNECTION_TYPE_FIELD_VALUE.name())
        .withField(POJO_CONNECTION_PROPERTY_GRADE_FIELD_NAME, POJO_CONNECTION_PROPERTY_GRADE_FIELD_VALUE)
        .withField(POJO_CONNECTION_TIME_FIELD_NAME, POJO_CONNECTION_TIME_FIELD_VALUE_AS_STRING)
        .withField(POJO_CONNECTION_IMPORTED_POJO_FIELD_NAME,
                   importedPojoValueDeclarer -> importedPojoValueDeclarer.asObjectValue()
                       .withField(IMPORTED_POJO_ID_FIELD_NAME, IMPORTED_POJO_ID_FIELD_VALUE)
                       .withField(IMPORTED_POJO_NAME_FIELD_NAME, IMPORTED_POJO_NAME_FIELD_VALUE)
                       .withField(IMPORTED_POJO_NUMBER_FIELD_NAME, IMPORTED_POJO_NUMBER_FIELD_VALUE.toString())
                       .withField(IMPORTED_POJO_BOOLEAN_FIELD_NAME, String.valueOf(IMPORTED_POJO_BOOLEAN_FIELD_VALUE)));

    testComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME, POJO_PARAMETER_NAME, valueDeclarerConsumer, POJO_PARAMETER_VALUE);
  }

  private void testComponentParameterization(String parameterGroupName, String parameterName,
                                             Consumer<ValueDeclarer> valueDeclarerConsumer, Object parameterValue)
      throws Exception {
    ResolverSet resolverSet =
        ResolverSetUtils.getResolverSetFromComponentParameterization(
                                                                     ComponentParameterization.builder(testParameterizedModel)
                                                                         .withParameter(parameterGroupName, parameterName,
                                                                                        valueDeclarerConsumer)
                                                                         .build(),
                                                                     muleContext, true, reflectionCache, expressionManager,
                                                                     testParameterizedModel.getName());
    ParameterGroupModel parameterGroupModel = testParameterizedModel.getParameterGroupModels().stream()
        .filter(pgm -> pgm.getName().equals(parameterGroupName)).findAny().get();
    ValueResolvingContext valueResolvingContext = ValueResolvingContext.builder(NullEventFactory.getNullEvent()).build();
    ResolverSetResult resolverSetResult = resolverSet.resolve(valueResolvingContext);
    if (parameterGroupModel.isShowInDsl()) {
      // assert different
    } else {
      assertThat(resolverSetResult.get(parameterName), is(parameterValue));
    }
  }

  private static ExtensionModel loadExtension(Class<?> clazz, ExtensionModelLoader loader) {
    Map<String, Object> params = SmallMap.of(TYPE_PROPERTY_NAME, clazz.getName(),
                                             VERSION, getProductVersion(),
                                             COMPILATION_MODE, true);

    final DslResolvingContext dslResolvingContext = getDefault(new LinkedHashSet<>());

    final String basePackage = clazz.getPackage().toString();
    final ClassLoader pluginClassLoader = new ClassLoader(clazz.getClassLoader()) {

      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith(basePackage)) {
          byte[] classBytes;
          try {
            classBytes =
                toByteArray(this.getClass().getResourceAsStream("/" + name.replaceAll("\\.", "/") + ".class"));
            return this.defineClass(null, classBytes, 0, classBytes.length);
          } catch (Exception e) {
            return super.loadClass(name);
          }
        } else {
          return super.loadClass(name, resolve);
        }
      }
    };

    return loader.loadExtensionModel(pluginClassLoader, dslResolvingContext, params);
  }



}
