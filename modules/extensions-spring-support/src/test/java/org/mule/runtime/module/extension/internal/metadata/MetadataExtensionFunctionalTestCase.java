/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.metadata.MetadataExtensionFunctionalTestCase.ResolutionType.DSL_RESOLUTION;
import static org.mule.runtime.module.extension.internal.metadata.MetadataExtensionFunctionalTestCase.ResolutionType.EXPLICIT_RESOLUTION;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.getMetadata;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.Typed;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataComponent;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataKey;
import org.mule.test.metadata.extension.MetadataExtension;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public abstract class MetadataExtensionFunctionalTestCase extends ExtensionFunctionalTestCase {

  protected static final String METADATA_TEST = "metadata-tests.xml";
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
  protected static final String NATIVE_QUERY_FLOW = "nativeQueryOperation";

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
  protected final static NullMetadataKey NULL_METADATA_KEY = new NullMetadataKey();
  protected final static ClassTypeLoader TYPE_LOADER = ExtensionsTestUtils.TYPE_LOADER;

  private static final MetadataComponentDescriptorProvider<OperationModel> explicitMetadataResolver =
      MetadataService::getOperationMetadata;
  private static final MetadataComponentDescriptorProvider<OperationModel> dslMetadataResolver =
      (metadataService, componentId, key) -> metadataService.getOperationMetadata(componentId);

  protected MetadataType personType;
  protected Location location;
  protected Event event;
  protected MetadataService metadataService;
  protected ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  protected BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JAVA);

  @Parameterized.Parameter
  public ResolutionType resolutionType = EXPLICIT_RESOLUTION;

  @Parameterized.Parameter(1)
  public MetadataComponentDescriptorProvider provider = explicitMetadataResolver;

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {MetadataExtension.class};
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {EXPLICIT_RESOLUTION, explicitMetadataResolver},
        {DSL_RESOLUTION, dslMetadataResolver}
    });
  }

  @Before
  public void setup() throws Exception {
    event = eventBuilder().message(InternalMessage.of("")).build();
    metadataService = muleContext.getRegistry().lookupObject(MuleMetadataService.class);
    personType = getMetadata(PERSON_METADATA_KEY.getId());
  }

  enum ResolutionType {
    EXPLICIT_RESOLUTION, DSL_RESOLUTION
  }

  <T extends ComponentModel<T>> MetadataResult<ComponentMetadataDescriptor<T>> getComponentDynamicMetadata(MetadataKey key) {
    checkArgument(location != null, "Unable to resolve Metadata. The location has not been configured.");
    return provider.resolveDynamicMetadata(metadataService, location, key);
  }

  ComponentMetadataDescriptor getSuccessComponentDynamicMetadata() {
    return getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);
  }

  <T extends ComponentModel<T>> ComponentMetadataDescriptor<T> getSuccessComponentDynamicMetadata(MetadataKey key) {
    MetadataResult<ComponentMetadataDescriptor<T>> componentMetadata = getComponentDynamicMetadata(key);
    String msg = componentMetadata.getFailures().stream().map(f -> "Failure: " + f.getMessage()).collect(joining(", "));
    assertThat(msg, componentMetadata.isSuccess(), is(true));
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

  void assertExpectedOutput(ComponentModel model, Type payloadType, Type attributesType) {
    assertExpectedOutput(model.getOutput(), model.getOutputAttributes(), TYPE_LOADER.load(payloadType),
                         TYPE_LOADER.load(attributesType));
  }

  void assertExpectedOutput(ComponentModel model, MetadataType payloadType, Type attributesType) {
    assertExpectedOutput(model.getOutput(), model.getOutputAttributes(), payloadType, TYPE_LOADER.load(attributesType));
  }

  void assertExpectedOutput(ComponentModel model, MetadataType payloadType, MetadataType attributesType) {
    assertExpectedOutput(model.getOutput(), model.getOutputAttributes(), payloadType, attributesType);
  }

  void assertExpectedOutput(OutputModel output, OutputModel attributes, MetadataType payloadType, MetadataType attributesType) {
    assertExpectedType(output.getType(), payloadType);
    assertExpectedType(attributes.getType(), attributesType);
  }

  private void assertExpectedType(MetadataType type, MetadataType expectedType) {
    assertThat(type, is(expectedType));
  }

  protected void assertExpectedType(Typed type, Type expectedType) {
    assertThat(type.getType(), is(TYPE_LOADER.load(expectedType)));
  }

  protected void assertExpectedType(Typed typedModel, MetadataType expectedType, boolean isDynamic) {
    assertThat(typedModel.getType(), is(expectedType));
    assertThat(typedModel.hasDynamicType(), is(isDynamic));
  }

  Set<MetadataKey> getKeysFromContainer(MetadataKeysContainer metadataKeysContainer) {
    return metadataKeysContainer.getKeys(metadataKeysContainer.getCategories().iterator().next()).get();
  }

  void assertSuccessResult(MetadataResult<?> result) {
    assertThat(result.getFailures(), is(empty()));
    String failures = result.getFailures().stream().map(Object::toString).collect(joining(", "));
    assertThat("Expecting success but this failure/s result/s found:\n " + failures, result.isSuccess(), is(true));
  }

  void assertFailureResult(MetadataResult<?> result, int failureNumber) {
    assertThat(result.getFailures(), hasSize(failureNumber));
    assertThat("Expecting failure but a success result found", result.isSuccess(), is(false));
  }

  interface MetadataComponentDescriptorProvider<T extends ComponentModel<T>> {

    MetadataResult<ComponentMetadataDescriptor<T>> resolveDynamicMetadata(MetadataService metadataService,
                                                                          Location location, MetadataKey key);
  }
}
