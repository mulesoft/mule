/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.metadata;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.mule.test.metadata.extension.MetadataConnection.CAR;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.getMetadata;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.SAN_FRANCISCO;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.USA;
import static org.mule.test.module.extension.metadata.MetadataExtensionFunctionalTestCase.ResolutionType.DSL_RESOLUTION;
import static org.mule.test.module.extension.metadata.MetadataExtensionFunctionalTestCase.ResolutionType.EXPLICIT_RESOLUTION;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.Typed;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataComponent;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataKey;
import org.mule.runtime.module.extension.api.metadata.MultilevelMetadataKeyBuilder;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.test.runner.RunnerDelegateTo;

import javax.inject.Inject;
import javax.inject.Named;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;

import org.junit.Before;
import org.junit.runners.Parameterized;

//TODO MULE-12809: Make MetadataTestCase use LazyMetadataService
@RunnerDelegateTo(Parameterized.class)
public abstract class MetadataExtensionFunctionalTestCase<T extends ComponentModel> extends AbstractExtensionFunctionalTestCase {

  protected static final String METADATA_TEST = "metadata-tests.xml";
  protected static final String RUNTIME_METADATA_CONFIG = "metadata-runtime-tests.xml";
  protected static final String DSQL_QUERY = "dsql:SELECT id FROM Circle WHERE (diameter < 18)";

  protected static final String METADATA_TEST_STATIC_NO_REF_CONFIGURATION = "metadata-tests-static-no-ref-configuration.xml";
  protected static final String METADATA_TEST_DYNAMIC_NO_REF_CONFIGURATION = "metadata-tests-dynamic-no-ref-configuration.xml";
  protected static final String METADATA_TEST_DYNAMIC_IMPLICIT_CONFIGURATION =
      "metadata-tests-dynamic-implicit-configuration.xml";

  protected static final String CONTENT_METADATA_WITH_KEY_ID = "contentMetadataWithKeyId";
  protected static final String OUTPUT_METADATA_WITH_KEY_ID = "outputMetadataWithKeyId";
  protected static final String CONTENT_AND_OUTPUT_METADATA_WITH_KEY_ID = "contentAndOutputMetadataWithKeyId";
  protected static final String OUTPUT_ONLY_WITHOUT_CONTENT_PARAM = "outputOnlyWithoutContentParam";
  protected static final String CONTENT_ONLY_IGNORES_OUTPUT = "contentOnlyIgnoresOutput";
  protected static final String CONTENT_METADATA_WITHOUT_KEY_ID = "contentMetadataWithoutKeyId";
  protected static final String OUTPUT_METADATA_WITHOUT_KEY_PARAM = "outputMetadataWithoutKeyId";
  protected static final String CONTENT_AND_OUTPUT_METADATA_WITHOUT_KEY_ID = "contentAndOutputMetadataWithoutKeyId";
  protected static final String CONTENT_METADATA_WITHOUT_KEYS_WITH_KEY_ID = "contentMetadataWithoutKeysWithKeyId";
  protected static final String OUTPUT_METADATA_WITHOUT_KEYS_WITH_KEY_ID = "outputMetadataWithoutKeysWithKeyId";
  protected static final String CONTENT_AND_OUTPUT_CACHE_RESOLVER = "contentAndOutputWithCacheResolver";
  protected static final String CONTENT_AND_OUTPUT_CACHE_RESOLVER_WITH_ALTERNATIVE_CONFIG =
      "contentAndOutputWithCacheResolverWithSpecificConfig";
  protected static final String QUERY_FLOW = "queryOperation";
  protected static final String QUERY_LIST_FLOW = "queryListOperation";
  protected static final String NATIVE_QUERY_FLOW = "nativeQueryOperation";
  protected static final String NATIVE_QUERY_LIST_FLOW = "nativeQueryListOperation";

  protected static final String CONTENT_ONLY_CACHE_RESOLVER = "contentOnlyCacheResolver";
  protected static final String OUTPUT_AND_METADATA_KEY_CACHE_RESOLVER = "outputAndMetadataKeyCacheResolver";
  protected static final String SOURCE_METADATA = "sourceMetadata";
  protected static final String SOURCE_METADATA_WITH_MULTILEVEL = "sourceMetadataWithMultilevel";
  protected static final String SHOULD_INHERIT_OPERATION_RESOLVERS = "shouldInheritOperationResolvers";
  protected static final String SHOULD_INHERIT_EXTENSION_RESOLVERS = "shouldInheritExtensionResolvers";
  protected static final String SHOULD_INHERIT_OPERATION_PARENT_RESOLVERS = "shouldInheritOperationParentResolvers";
  protected static final String SIMPLE_MULTILEVEL_KEY_RESOLVER = "simpleMultiLevelKeyResolver";
  protected static final String INCOMPLETE_MULTILEVEL_KEY_RESOLVER = "incompleteMultiLevelKeyResolver";
  protected static final String TYPE_WITH_DECLARED_SUBTYPES_METADATA = "typeWithDeclaredSubtypesMetadata";
  protected static final String RESOLVER_WITH_DYNAMIC_CONFIG = "resolverWithDynamicConfig";
  protected static final String RESOLVER_WITH_IMPLICIT_DYNAMIC_CONFIG = "resolverWithImplicitDynamicConfig";
  protected static final String RESOLVER_WITH_IMPLICIT_STATIC_CONFIG = "resolverWithImplicitStaticConfig";
  protected static final String OUTPUT_ATTRIBUTES_WITH_DYNAMIC_METADATA = "outputAttributesWithDynamicMetadata";
  protected static final String OUTPUT_ATTRIBUTES_WITH_DECLARED_SUBTYPES_METADATA =
      "outputAttributesWithDeclaredSubtypesMetadata";
  protected static final String RESOLVER_CONTENT_WITH_CONTEXT_CLASSLOADER = "resolverContentWithContextClassLoader";
  protected static final String RESOLVER_OUTPUT_WITH_CONTEXT_CLASSLOADER = "resolverOutputWithContextClassLoader";
  protected static final String ENUM_METADATA_KEY = "enumMetadataKey";
  protected static final String BOOLEAN_METADATA_KEY = "booleanMetadataKey";
  protected static final String METADATA_KEY_DEFAULT_VALUE = "metadataKeyDefaultValue";
  protected static final String MULTILEVEL_METADATA_KEY_DEFAULT_VALUE = "multilevelMetadataKeyDefaultValue";
  protected static final String OUTPUT_AND_MULTIPLE_INPUT_WITH_KEY_ID = "outputAndMultipleInputWithKeyId";

  protected static final String CONTINENT = "continent";
  protected static final String COUNTRY = "country";
  protected static final String CITY = "city";

  protected final static MetadataKey PERSON_METADATA_KEY = newKey(PERSON).build();
  protected static final MetadataKey CAR_KEY = newKey(CAR).build();
  protected static final MetadataKey LOCATION_MULTILEVEL_KEY =
      MultilevelMetadataKeyBuilder.newKey(AMERICA, CONTINENT).withChild(MultilevelMetadataKeyBuilder.newKey(USA, COUNTRY)
          .withChild(MultilevelMetadataKeyBuilder.newKey(SAN_FRANCISCO, CITY))).build();

  protected final static NullMetadataKey NULL_METADATA_KEY = new NullMetadataKey();
  protected final static ClassTypeLoader TYPE_LOADER = ExtensionsTestUtils.TYPE_LOADER;

  @Inject
  @Named(METADATA_SERVICE_KEY)
  protected MetadataService metadataService;

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  public boolean disableXmlValidations() {
    return true;
  }

  protected MetadataType personType;
  protected Location location;
  protected CoreEvent event;
  protected ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  protected BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JAVA);
  protected MetadataComponentDescriptorProvider<T> provider;

  protected ResolutionType resolutionType;

  MetadataExtensionFunctionalTestCase(ResolutionType resolutionType) {
    this.resolutionType = resolutionType;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {EXPLICIT_RESOLUTION},
        {DSL_RESOLUTION}
    });
  }

  @Before
  public void setup() throws Exception {
    event = CoreEvent.builder(create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION)).message(of("")).build();
    personType = getMetadata(PERSON_METADATA_KEY.getId());
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  public enum ResolutionType {
    EXPLICIT_RESOLUTION, DSL_RESOLUTION
  }

  MetadataResult<ComponentMetadataDescriptor<T>> getComponentDynamicMetadata(MetadataKey key) {
    checkArgument(location != null, "Unable to resolve Metadata. The location has not been configured.");
    return provider.resolveDynamicMetadata(metadataService, location, key);
  }

  ComponentMetadataDescriptor getSuccessComponentDynamicMetadata() {
    return getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);
  }

  ComponentMetadataDescriptor<T> getSuccessComponentDynamicMetadataWithKey(MetadataKey key) {
    return getSuccessComponentDynamicMetadata(key, this::assertResolvedKey);
  }

  ComponentMetadataDescriptor<T> getSuccessComponentDynamicMetadata(MetadataKey key) {
    return getSuccessComponentDynamicMetadata(key, (a, b) -> {
    });
  }

  private ComponentMetadataDescriptor<T> getSuccessComponentDynamicMetadata(MetadataKey key,
                                                                            BiConsumer<MetadataResult<ComponentMetadataDescriptor<T>>, MetadataKey> assertKeys) {
    MetadataResult<ComponentMetadataDescriptor<T>> componentMetadata = getComponentDynamicMetadata(key);
    String msg = componentMetadata.getFailures().stream().map(f -> "Failure: " + f.getMessage()).collect(joining(", "));
    assertThat(msg, componentMetadata.isSuccess(), is(true));
    assertKeys.accept(componentMetadata, key);
    return componentMetadata.get();
  }

  void assertMetadataFailure(MetadataFailure failure,
                             String msgContains,
                             FailureCode failureCode,
                             String traceContains,
                             MetadataComponent failingComponent) {
    assertMetadataFailure(failure, msgContains, failureCode, traceContains, failingComponent, "");
  }

  void assertMetadataFailure(MetadataFailure failure,
                             String msgContains,
                             FailureCode failureCode,
                             String traceContains,
                             MetadataComponent failingComponent,
                             String failingElement) {
    assertThat(failure.getFailureCode(), is(failureCode));
    if (!isBlank(msgContains)) {
      assertThat(failure.getMessage(), containsString(msgContains));
    }
    if (!isBlank(traceContains)) {
      assertThat(failure.getReason(), containsString(traceContains));
    }
    assertThat(failure.getFailingComponent(), is(failingComponent));
    if (!isBlank(failingElement)) {
      assertThat(failure.getFailingElement().isPresent(), is(true));
      assertThat(failure.getFailingElement().get(), is(failingElement));
    }
  }

  void assertExpectedOutput(ConnectableComponentModel model, Type payloadType, Type attributesType) {
    assertExpectedOutput(model.getOutput(), model.getOutputAttributes(), TYPE_LOADER.load(payloadType),
                         TYPE_LOADER.load(attributesType));
  }

  void assertExpectedOutput(ConnectableComponentModel model, MetadataType payloadType, Type attributesType) {
    assertExpectedOutput(model.getOutput(), model.getOutputAttributes(), payloadType, TYPE_LOADER.load(attributesType));
  }

  void assertExpectedOutput(ConnectableComponentModel model, MetadataType payloadType, MetadataType attributesType) {
    assertExpectedOutput(model.getOutput(), model.getOutputAttributes(), payloadType, attributesType);
  }

  void assertExpectedOutput(OutputModel output, OutputModel attributes, MetadataType payloadType, MetadataType attributesType) {
    assertExpectedType(output.getType(), payloadType);
    assertExpectedType(attributes.getType(), attributesType);
  }

  protected void assertExpectedType(MetadataType type, MetadataType expectedType) {
    assertThat(type, is(expectedType));
  }

  protected void assertExpectedType(Typed type, Type expectedType) {
    assertThat(type.getType(), is(TYPE_LOADER.load(expectedType)));
  }

  protected void assertExpectedType(Typed typedModel, MetadataType expectedType, boolean isDynamic) {
    assertThat(typedModel.getType(), is(expectedType));
    assertThat(typedModel.hasDynamicType(), is(isDynamic));
  }

  protected <T extends ComponentModel> void assertResolvedKey(MetadataResult<ComponentMetadataDescriptor<T>> result,
                                                              MetadataKey metadataKey) {
    assertThat(result.get().getMetadataAttributes().getKey().isPresent(), is(true));
    MetadataKey resultKey = result.get().getMetadataAttributes().getKey().get();
    assertSameKey(metadataKey, resultKey);

    MetadataKey child = metadataKey.getChilds().stream().findFirst().orElseGet(() -> null);
    MetadataKey otherChild = resultKey.getChilds().stream().findFirst().orElseGet(() -> null);
    while (child != null && otherChild != null) {
      assertSameKey(child, otherChild);
      child = child.getChilds().stream().findFirst().orElseGet(() -> null);
      otherChild = otherChild.getChilds().stream().findFirst().orElseGet(() -> null);
    }
    assertThat(child == null && otherChild == null, is(true));
  }

  private void assertSameKey(MetadataKey metadataKey, MetadataKey resultKey) {
    assertThat(resultKey.getId(), is(metadataKey.getId()));
    assertThat(resultKey.getChilds(), hasSize(metadataKey.getChilds().size()));
  }

  public Set<MetadataKey> getKeysFromContainer(MetadataKeysContainer metadataKeysContainer) {
    return metadataKeysContainer.getKeys(metadataKeysContainer.getCategories().iterator().next()).get();
  }

  public void assertSuccessResult(MetadataResult<?> result) {
    assertThat(result.getFailures(), is(empty()));
    String failures = result.getFailures().stream().map(Object::toString).collect(joining(", "));
    assertThat("Expecting success but this failure/s result/s found:\n " + failures, result.isSuccess(), is(true));
  }

  void assertFailureResult(MetadataResult<?> result, int failureNumber) {
    assertThat(result.getFailures(), hasSize(failureNumber));
    assertThat("Expecting failure but a success result found", result.isSuccess(), is(false));
  }

  interface MetadataComponentDescriptorProvider<T extends ComponentModel> {

    MetadataResult<ComponentMetadataDescriptor<T>> resolveDynamicMetadata(MetadataService metadataService,
                                                                          Location location, MetadataKey key);
  }
}
