/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.metadata.MetadataExtensionFunctionalTestCase.ResolutionType.DSL_RESOLUTION;
import static org.mule.runtime.module.extension.internal.metadata.MetadataExtensionFunctionalTestCase.ResolutionType.EXPLICIT_RESOLUTION;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.getMetadata;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.ComponentId;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ParameterMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataKey;
import org.mule.test.metadata.extension.MetadataExtension;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;

@RunWith(Parameterized.class)
public abstract class MetadataExtensionFunctionalTestCase extends ExtensionFunctionalTestCase {

  protected static final String FIRST_PROCESSOR_INDEX = "0";

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

  private static final MetadataComponentDescriptorProvider EXPLICIT_METADATA_RESOLVER = MetadataService::getMetadata;
  private static final MetadataComponentDescriptorProvider DSL_METADATA_RESOLVER =
      (MetadataComponentDescriptorProvider) (metadataService, componentId, key) -> metadataService
          .getMetadata(componentId);


  protected MetadataType personType;
  protected ComponentId componentId;
  protected Event event;
  protected MetadataService metadataService;
  protected ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  protected BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JAVA);

  @Parameterized.Parameter
  public ResolutionType resolutionType;

  @Parameterized.Parameter(1)
  public MetadataComponentDescriptorProvider provider;

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {MetadataExtension.class};
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {EXPLICIT_RESOLUTION, EXPLICIT_METADATA_RESOLVER},
        {DSL_RESOLUTION, DSL_METADATA_RESOLVER}
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

  MetadataResult<ComponentMetadataDescriptor> getComponentDynamicMetadata(MetadataKey key) {
    checkArgument(componentId != null, "Unable to resolve Metadata. The Component ID has not been configured.");
    return provider.resolveDynamicMetadata(metadataService, componentId, key);
  }

  ComponentMetadataDescriptor getSuccessComponentDynamicMetadata() {
    return getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);
  }

  ComponentMetadataDescriptor getSuccessComponentDynamicMetadata(MetadataKey key) {
    MetadataResult<ComponentMetadataDescriptor> componentMetadata = getComponentDynamicMetadata(key);
    assertThat(componentMetadata.getFailure().isPresent() ? componentMetadata.getFailure().get().getReason() : "No Failure",
               componentMetadata.isSuccess(), is(true));

    return componentMetadata.get();
  }

  protected void assertFailure(MetadataResult<?> result, String msgContains, FailureCode failureCode, String traceContains)
      throws IOException {
    assertThat(result.isSuccess(), is(false));
    Optional<MetadataFailure> metadataFailure = result.getFailure();
    assertThat(metadataFailure.get().getMessage(), metadataFailure.get().getFailureCode(), is(failureCode));

    if (!StringUtils.isBlank(msgContains)) {
      assertThat(metadataFailure.get().getMessage(), containsString(msgContains));
    }

    if (!StringUtils.isBlank(traceContains)) {
      assertThat(metadataFailure.get().getReason(), containsString(traceContains));
    }
  }

  protected void assertExpectedOutput(MetadataResult<OutputMetadataDescriptor> outputDescriptor, Type payloadType,
                                      Type attributesType)
      throws IOException {
    assertExpectedType(outputDescriptor.get().getPayloadMetadata(), payloadType);
    assertExpectedType(outputDescriptor.get().getAttributesMetadata(), attributesType);
  }

  protected void assertExpectedOutput(MetadataResult<OutputMetadataDescriptor> outputDescriptor, MetadataType payloadType,
                                      Type attributesType)
      throws IOException {
    assertExpectedType(outputDescriptor.get().getPayloadMetadata(), payloadType);
    assertExpectedType(outputDescriptor.get().getAttributesMetadata(), attributesType);
  }

  protected void assertExpectedOutput(MetadataResult<OutputMetadataDescriptor> outputDescriptor, MetadataType payloadType,
                                      MetadataType attributesType)
      throws IOException {
    assertExpectedType(outputDescriptor.get().getPayloadMetadata(), payloadType);
    assertExpectedType(outputDescriptor.get().getAttributesMetadata(), attributesType);
  }

  protected void assertExpectedType(MetadataResult<TypeMetadataDescriptor> descriptor, Type type) throws IOException {
    assertThat(descriptor.get().getType(), is(TYPE_LOADER.load(type)));
  }

  protected void assertExpectedType(MetadataResult<ParameterMetadataDescriptor> descriptor, String name, Type type)
      throws IOException {
    assertThat(descriptor.get().getType(), is(TYPE_LOADER.load(type)));
    if (!StringUtils.isBlank(name)) {
      assertThat(descriptor.get().getName(), is(name));
    }
    assertThat(descriptor.get().isDynamic(), is(false));
  }

  protected void assertExpectedType(MetadataResult<TypeMetadataDescriptor> descriptor, MetadataType type) throws IOException {
    assertThat(descriptor.get().getType(), is(type));
  }

  protected void assertExpectedType(MetadataResult<ParameterMetadataDescriptor> descriptor, String name, MetadataType type,
                                    boolean isDynamic)
      throws IOException {
    assertThat(descriptor.isSuccess(), is(true));
    assertThat(descriptor.get().getType(), is(type));
    if (!StringUtils.isBlank(name)) {
      assertThat(descriptor.get().getName(), is(name));
    }
    assertThat(descriptor.get().isDynamic(), is(isDynamic));
  }

  protected Set<MetadataKey> getKeysFromContainer(MetadataKeysContainer metadataKeysContainer) {
    return metadataKeysContainer.getKeys(metadataKeysContainer.getCategories().iterator().next()).get();
  }

  protected void assertSuccess(MetadataResult<?> metadata) {
    if (!metadata.isSuccess()) {
      if (metadata.getFailure().isPresent()) {
        fail(metadata.getFailure().get().getFailureCode() + " : " +
            metadata.getFailure().get().getMessage() + " : " +
            metadata.getFailure().get().getReason());
      }

      fail("Expected result to be success but it failed without error information");
    }
  }

  private interface MetadataComponentDescriptorProvider {

    MetadataResult<ComponentMetadataDescriptor> resolveDynamicMetadata(MetadataService metadataService,
                                                                       ComponentId componentId, MetadataKey key);
  }
}
