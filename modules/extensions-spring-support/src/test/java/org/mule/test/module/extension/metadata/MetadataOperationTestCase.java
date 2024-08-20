/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.metadata;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.api.model.MetadataFormat.JSON;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.tck.junit4.matcher.MetadataKeyMatcher.metadataKeyWithId;
import static org.mule.test.metadata.extension.MetadataConnection.CAR;
import static org.mule.test.metadata.extension.MetadataConnection.HOUSE;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.BRAND;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.TIRES;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.ARGENTINA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.BUENOS_AIRES;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.EUROPE;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.LA_PLATA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.USA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.USA_DISPLAY_NAME;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_BUILDER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.assertMessageType;

import static java.util.Collections.singletonMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

import org.mule.functional.listener.Callback;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.model.VoidType;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.RouterOutputMetadataContext;
import org.mule.runtime.api.metadata.ScopeOutputMetadataContext;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.InputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.RouterInputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ScopeInputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.extension.api.metadata.NullMetadataKey;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ParameterTypeWrapper;
import org.mule.tck.junit4.matcher.MetadataKeyMatcher;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.model.animals.Animal;
import org.mule.test.metadata.extension.model.animals.AnimalClade;
import org.mule.test.metadata.extension.model.animals.Bear;
import org.mule.test.metadata.extension.model.animals.SwordFish;
import org.mule.test.metadata.extension.model.attribute.AbstractOutputAttributes;
import org.mule.test.metadata.extension.model.shapes.Rectangle;
import org.mule.test.metadata.extension.model.shapes.Shape;
import org.mule.test.metadata.extension.resolver.TestThreadContextClassLoaderResolver;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.Test;

import io.qameta.allure.Issue;

public class MetadataOperationTestCase extends AbstractMetadataOperationTestCase {

  private static final String MESSAGE_ATTRIBUTES_PERSON_TYPE_METADATA = "messageAttributesPersonTypeMetadata";
  private static final String MESSAGE_ATTRIBUTES_ANY_TYPE_METADATA = "messageAttributesAnyTypeMetadata";
  private static final String PAGED_OPERATION_METADATA = "pagedOperationMetadata";
  private static final String PAGED_OPERATION_METADATA_RESULT = "pagedOperationMetadataResult";
  private static final String PAGED_OPERATION_METADATA_RESULT_WITH_ATTRIBUTES =
      "pagedOperationMetadataResultWithAttributesResolver";

  public MetadataOperationTestCase(ResolutionType resolutionType) {
    super(resolutionType);
  }

  @Override
  protected String getConfigFile() {
    return METADATA_TEST;
  }

  @Test
  public void getMetadataKeysWithKeyId() throws Exception {
    location = Location.builder().globalName(OUTPUT_METADATA_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    assertSuccessResult(metadataKeysResult);
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys.size(), is(3));
    assertThat(metadataKeys, hasItems(metadataKeyWithId(PERSON), metadataKeyWithId(CAR), metadataKeyWithId(HOUSE)));
  }

  @Test
  public void getMetadataKeysWithoutKeyId() throws Exception {
    location = Location.builder().globalName(CONTENT_METADATA_WITHOUT_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<MetadataKeysContainer> metadataKeys = metadataService.getMetadataKeys(location);
    assertSuccessResult(metadataKeys);
    final Set<MetadataKey> keys = getKeysFromContainer(metadataKeys.get());
    assertThat(keys.size(), is(1));
    assertThat(keys.iterator().next(), instanceOf(NullMetadataKey.class));
  }

  @Test
  public void getMultilevelKeys() throws Exception {
    location = Location.builder().globalName(SIMPLE_MULTILEVEL_KEY_RESOLVER).addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    assertSuccessResult(metadataKeysResult);
    final Set<MetadataKey> continents = getKeysFromContainer(metadataKeysResult.get());
    assertThat(continents, hasSize(2));

    // preserve order
    assertThat(continents, hasItems(metadataKeyWithId(AMERICA).withDisplayName(AMERICA).withPartName(CONTINENT),
                                    metadataKeyWithId(EUROPE).withDisplayName(EUROPE).withPartName(CONTINENT)));

    // preserve order
    MetadataKey americaKey = continents.iterator().next();
    assertThat(americaKey.getChilds(), hasItems(metadataKeyWithId(ARGENTINA).withDisplayName(ARGENTINA).withPartName(COUNTRY),
                                                metadataKeyWithId(USA).withDisplayName(USA_DISPLAY_NAME).withPartName(COUNTRY)));

    // preserve order
    MetadataKey argentinaKey = americaKey.getChilds().iterator().next();
    assertThat(argentinaKey.getChilds(),
               hasItems(metadataKeyWithId(BUENOS_AIRES).withDisplayName(BUENOS_AIRES).withPartName(CITY),
                        metadataKeyWithId(LA_PLATA).withDisplayName(LA_PLATA).withPartName(CITY)));
  }

  @Test
  public void partialMultilevelKeys() throws Exception {
    location = Location.builder().globalName("partialMultiLevelKeyResolver").addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    assertSuccessResult(metadataKeysResult);
    final Set<MetadataKey> continents = getKeysFromContainer(metadataKeysResult.get());
    assertThat(continents, hasSize(1));

    assertThat(continents, hasItem(metadataKeyWithId(AMERICA).withDisplayName(AMERICA).withPartName(CONTINENT)));
    assertThat(continents, not(hasItem(metadataKeyWithId(EUROPE).withDisplayName(EUROPE).withPartName(CONTINENT))));
  }

  @Test
  public void partialMultilevelKeysExplicitResolution() throws Exception {
    location = Location.builder().globalName(EMPTY_PARTIAL_MULTILEVEL_KEYS).addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<MetadataKeysContainer> metadataKeysResult =
        metadataService.getMetadataKeys(location, MetadataKeyBuilder.newKey(AMERICA).withPartName(CONTINENT).build());
    assertSuccessResult(metadataKeysResult);
    final Set<MetadataKey> continents = getKeysFromContainer(metadataKeysResult.get());
    assertThat(continents, hasSize(1));

    assertThat(continents, hasItem(metadataKeyWithId(AMERICA).withDisplayName(AMERICA).withPartName(CONTINENT)));
    assertThat(continents, not(hasItem(metadataKeyWithId(EUROPE).withDisplayName(EUROPE).withPartName(CONTINENT))));
  }

  @Test
  public void emptyPartialMultilevelKeys() throws Exception {
    location = Location.builder().globalName(EMPTY_PARTIAL_MULTILEVEL_KEYS).addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    assertSuccessResult(metadataKeysResult);
    final Set<MetadataKey> continents = getKeysFromContainer(metadataKeysResult.get());
    assertThat(continents, hasSize(2));

    assertThat(continents, hasItem(metadataKeyWithId(AMERICA).withDisplayName(AMERICA).withPartName(CONTINENT)));
    assertThat(continents, hasItem(metadataKeyWithId(EUROPE).withDisplayName(EUROPE).withPartName(CONTINENT)));
  }

  @Test
  public void twoLevelPartialMultilevelKeys() throws Exception {
    location = Location.builder().globalName("twoLevelPartialMultiLevelKeyResolver").addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    assertSuccessResult(metadataKeysResult);
    final Set<MetadataKey> continents = getKeysFromContainer(metadataKeysResult.get());
    assertThat(continents, hasSize(1));

    Set<MetadataKey> countries = continents.iterator().next().getChilds();
    assertThat(countries, hasSize(1));

    Set<MetadataKey> cities = countries.iterator().next().getChilds();
    assertThat(cities, hasSize(2));

    assertThat(cities, hasItem(metadataKeyWithId(BUENOS_AIRES).withPartName(CITY)));
    assertThat(cities, hasItem(metadataKeyWithId(LA_PLATA).withPartName(CITY)));
  }

  @Test
  public void twoLevelPartialMultilevelKeysExplicitResolution() throws Exception {
    location = Location.builder().globalName(EMPTY_PARTIAL_MULTILEVEL_KEYS).addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService
        .getMetadataKeys(location, MetadataKeyBuilder
            .newKey(AMERICA)
            .withPartName(CONTINENT)
            .withChild(MetadataKeyBuilder.newKey(ARGENTINA)
                .withPartName(COUNTRY))
            .build());
    assertSuccessResult(metadataKeysResult);
    final Set<MetadataKey> continents = getKeysFromContainer(metadataKeysResult.get());
    assertThat(continents, hasSize(1));

    Set<MetadataKey> countries = continents.iterator().next().getChilds();
    assertThat(countries, hasSize(1));

    Set<MetadataKey> cities = countries.iterator().next().getChilds();
    assertThat(cities, hasSize(2));

    assertThat(cities, hasItem(metadataKeyWithId(BUENOS_AIRES).withPartName(CITY)));
    assertThat(cities, hasItem(metadataKeyWithId(LA_PLATA).withPartName(CITY)));
  }

  @Test
  public void injectComposedMetadataKeyIdInstanceInMetadataResolver() throws Exception {
    location = Location.builder().globalName(SIMPLE_MULTILEVEL_KEY_RESOLVER).addProcessorsPart().addIndexPart(0).build();
    MetadataKey key = LOCATION_MULTILEVEL_KEY;
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadataResult =
        metadataService.getOperationMetadata(location, key);
    assertSuccessResult(metadataResult);
    assertResolvedKey(metadataResult, LOCATION_MULTILEVEL_KEY);
  }

  @Test
  public void dynamicOperationMetadata() throws Exception {
    location = Location.builder().globalName(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "type"), STRING_TYPE);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void outputAndMultipleInputWithKeyId() throws Exception {
    location = Location.builder().globalName(OUTPUT_AND_MULTIPLE_INPUT_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "type"), STRING_TYPE);
    assertExpectedType(getParameter(typedModel, "firstPerson"), personType, true);
    assertExpectedType(getParameter(typedModel, "otherPerson"), personType, true);
  }

  @Test
  public void multipleInputWithKeyIdExplicitParameterResolution() throws Exception {
    location = Location.builder().globalName(OUTPUT_AND_MULTIPLE_INPUT_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<InputMetadataDescriptor> inputMetadataResult = metadataService.getInputMetadata(location, PERSON_METADATA_KEY);
    assertThat(inputMetadataResult.isSuccess(), is(true));
    InputMetadataDescriptor inputMetadataDescriptor = inputMetadataResult.get();
    assertExpectedParameterMetadataDescriptor(inputMetadataDescriptor.getParameterMetadata("type"),
                                              STRING_TYPE, false);
    assertExpectedParameterMetadataDescriptor(inputMetadataDescriptor.getParameterMetadata("firstPerson"), personType);
    assertExpectedParameterMetadataDescriptor(inputMetadataDescriptor.getParameterMetadata("otherPerson"), personType);
  }

  @Test
  @Issue("W-15158118")
  public void outputMultipleInputWithKeyIdExplicitParameterResolution() throws Exception {
    location = Location.builder().globalName(OUTPUT_AND_MULTIPLE_INPUT_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<OutputMetadataDescriptor> outputMetadataResult = metadataService.getOutputMetadata(location, CAR_KEY);
    assertThat(outputMetadataResult.isSuccess(), is(true));
    OutputMetadataDescriptor outputMetadataDescriptor = outputMetadataResult.get();
    assertThat(outputMetadataDescriptor.getPayloadMetadata().isDynamic(), is(true));
    assertExpectedType(outputMetadataDescriptor.getPayloadMetadata().getType(), carType);
  }

  @Test
  @Issue("W-15158118")
  public void outputResolverForScope() throws Exception {
    location = Location.builder().globalName(SCOPE_WITH_OUTPUT_RESOLVER).addProcessorsPart().addIndexPart(0).build();

    MessageMetadataType chainOutputMessageType = MessageMetadataType.builder()
        .payload(carType)
        .build();

    ScopeOutputMetadataContext scopeContext = new TestScopeOutputMetadataContext(chainOutputMessageType);
    MetadataResult<OutputMetadataDescriptor> outputMetadataResult =
        metadataService.getScopeOutputMetadata(location, CAR_KEY, scopeContext);

    assertThat(outputMetadataResult.isSuccess(), is(true));
    OutputMetadataDescriptor outputMetadataDescriptor = outputMetadataResult.get();
    assertThat(outputMetadataDescriptor.getPayloadMetadata().isDynamic(), is(true));
    assertExpectedType(outputMetadataDescriptor.getPayloadMetadata().getType(), carType);
  }

  @Test
  public void passThroughOutputResolverForScope() {
    location = Location.builder().globalName(SCOPE_WITH_PASS_THROUGH_OUTPUT_RESOLVER).addProcessorsPart().addIndexPart(0).build();

    MessageMetadataType chainOutputMessageType = MessageMetadataType.builder()
        .payload(carType)
        .attributes(VOID_TYPE)
        .build();

    ScopeOutputMetadataContext scopeContext = new TestScopeOutputMetadataContext(chainOutputMessageType);
    MetadataResult<OutputMetadataDescriptor> outputMetadataResult =
        metadataService.getScopeOutputMetadata(location, CAR_KEY, scopeContext);

    assertThat(outputMetadataResult.getFailures(), is(empty()));
    OutputMetadataDescriptor outputMetadataDescriptor = outputMetadataResult.get();
    assertThat(outputMetadataDescriptor.getPayloadMetadata().isDynamic(), is(true));
    assertExpectedType(outputMetadataDescriptor.getPayloadMetadata().getType(), carType);
    assertThat(outputMetadataDescriptor.getAttributesMetadata().isDynamic(), is(true));
    assertExpectedType(outputMetadataDescriptor.getAttributesMetadata().getType(), VOID_TYPE);
  }

  @Test
  @Issue("W-14969942")
  public void passthroughInputResolverForScope() throws Exception {
    location = Location.builder().globalName(SCOPE_WITH_OUTPUT_RESOLVER).addProcessorsPart().addIndexPart(0).build();

    MessageMetadataType inputMessageType = MessageMetadataType.builder()
        .payload(carType)
        .attributes(VOID_TYPE)
        .build();

    MetadataResult<ScopeInputMetadataDescriptor> inputMetadata =
        metadataService.getScopeInputMetadata(location, CAR_KEY, () -> inputMessageType);

    assertThat(inputMetadata.isSuccess(), is(true));
    ScopeInputMetadataDescriptor metadataDescriptor = inputMetadata.get();
    assertThat(metadataDescriptor.getChainInputMessageType(), is(sameInstance(inputMessageType)));
  }

  @Test
  @Issue("W-14969942")
  public void inputResolverForScope() throws Exception {
    location = Location.builder().globalName(SCOPE_WITH_INPUT_RESOLVER).addProcessorsPart().addIndexPart(0).build();

    MessageMetadataType inputMessageType = MessageMetadataType.builder()
        .payload(typeBuilder.withFormat(JAVA).objectType().build())
        .attributes(typeBuilder.withFormat(JAVA).objectType().build())
        .build();

    MetadataResult<ScopeInputMetadataDescriptor> inputMetadata =
        metadataService.getScopeInputMetadata(location, null, () -> inputMessageType);

    assertThat(inputMetadata.isSuccess(), is(true));
    ScopeInputMetadataDescriptor metadataDescriptor = inputMetadata.get();

    MessageMetadataType chainInputMessageType = metadataDescriptor.getChainInputMessageType();
    assertThat(chainInputMessageType.getPayloadType().get(),
               is(sameInstance(metadataDescriptor.getParameterMetadata("jsonValue").getType())));
    assertThat(chainInputMessageType.getPayloadType().get().getMetadataFormat(), is(JSON));
    assertThat(chainInputMessageType.getAttributesType().get(), is(instanceOf(VoidType.class)));
  }

  @Test
  @Issue("W-16408471")
  public void scopeWithOnlyChainInputResolver() {
    location = builderFromStringRepresentation("scopeWithOnlyChainInputResolver/processors/0").build();

    MessageMetadataType inputMessageType = MessageMetadataType.builder()
        .payload(typeBuilder.withFormat(JAVA).objectType().build())
        .attributes(typeBuilder.withFormat(JAVA).objectType().build())
        .build();

    MetadataResult<ScopeInputMetadataDescriptor> inputMetadataResult =
        metadataService.getScopeInputMetadata(location, null, () -> inputMessageType);

    assertThat(inputMetadataResult.getFailures(), is(empty()));
    ScopeInputMetadataDescriptor inputMetadata = inputMetadataResult.get();

    MessageMetadataType routeInputType = inputMetadata.getChainInputMessageType();
    assertThat(routeInputType.getPayloadType().get(), is(STRING_TYPE));
  }

  @Test
  public void outputResolverForRouter() throws Exception {
    location = Location.builder().globalName(ROUTER_WITH_METADATA_RESOLVER).addProcessorsPart().addIndexPart(0).build();
    RouterOutputMetadataContext routerOutputMetadataContext = new RouterOutputMetadataContext() {

      @Override
      public Map<String, Supplier<MessageMetadataType>> getRouteOutputMessageTypes() {
        return singletonMap("metaroute", () -> MessageMetadataType.builder().payload(carType).build());
      }

      @Override
      public Supplier<MessageMetadataType> getRouterInputMessageType() {
        return () -> null;
      }
    };
    MetadataResult<OutputMetadataDescriptor> outputMetadataResult =
        metadataService.getRouterOutputMetadata(location, CAR_KEY, routerOutputMetadataContext);
    assertThat(outputMetadataResult.isSuccess(), is(true));
    OutputMetadataDescriptor outputMetadataDescriptor = outputMetadataResult.get();
    assertThat(outputMetadataDescriptor.getPayloadMetadata().isDynamic(), is(true));
    assertExpectedType(outputMetadataDescriptor.getPayloadMetadata().getType(), carType);
  }

  @Test
  public void oneOfRoutesOutputResolverForRouter() {
    location =
        Location.builder().globalName(ROUTER_WITH_ONE_OF_ROUTES_METADATA_RESOLVER).addProcessorsPart().addIndexPart(0).build();

    Map<String, Supplier<MessageMetadataType>> routeOutputTypes = new LinkedHashMap<>();
    routeOutputTypes.put("metaroute1", () -> MessageMetadataType.builder().payload(carType).build());
    routeOutputTypes.put("metaroute2", () -> MessageMetadataType.builder().payload(personType).build());

    RouterOutputMetadataContext routerOutputMetadataContext = new TestRouterOutputMetadataContext(routeOutputTypes);
    MetadataResult<OutputMetadataDescriptor> outputMetadataResult =
        metadataService.getRouterOutputMetadata(location, CAR_KEY, routerOutputMetadataContext);

    assertThat(outputMetadataResult.getFailures(), is(empty()));
    OutputMetadataDescriptor outputMetadataDescriptor = outputMetadataResult.get();
    assertThat(outputMetadataDescriptor.getPayloadMetadata().isDynamic(), is(true));

    MetadataType expectedPayloadType = typeBuilder.unionType()
        .of(carType)
        .of(personType)
        .build();
    assertExpectedType(outputMetadataDescriptor.getPayloadMetadata().getType(), expectedPayloadType);
    assertExpectedType(outputMetadataDescriptor.getAttributesMetadata().getType(), VOID_TYPE);
  }

  @Test
  public void allOfRoutesOutputResolverForRouter() {
    location =
        Location.builder().globalName(ROUTER_WITH_ALL_OF_ROUTES_METADATA_RESOLVER).addProcessorsPart().addIndexPart(0).build();

    Map<String, Supplier<MessageMetadataType>> routeOutputTypes = new LinkedHashMap<>();
    routeOutputTypes.put("metaroute1", () -> MessageMetadataType.builder().payload(carType).build());
    routeOutputTypes.put("metaroute2", () -> MessageMetadataType.builder().payload(personType).build());

    RouterOutputMetadataContext routerOutputMetadataContext = new TestRouterOutputMetadataContext(routeOutputTypes);
    MetadataResult<OutputMetadataDescriptor> outputMetadataResult =
        metadataService.getRouterOutputMetadata(location, CAR_KEY, routerOutputMetadataContext);

    assertThat(outputMetadataResult.getFailures(), is(empty()));
    OutputMetadataDescriptor outputMetadataDescriptor = outputMetadataResult.get();
    assertThat(outputMetadataDescriptor.getPayloadMetadata().isDynamic(), is(true));

    ObjectTypeBuilder objectTypeBuilder = typeBuilder.objectType();
    objectTypeBuilder.addField().key("metaroute1").value(routeOutputTypes.get("metaroute1").get());
    objectTypeBuilder.addField().key("metaroute2").value(routeOutputTypes.get("metaroute2").get());
    MetadataType expectedPayloadType = objectTypeBuilder.build();
    assertExpectedType(outputMetadataDescriptor.getPayloadMetadata().getType(), expectedPayloadType);
    assertExpectedType(outputMetadataDescriptor.getAttributesMetadata().getType(), VOID_TYPE);
  }

  @Test
  public void inputResolverForRouter() throws Exception {
    location = Location.builder().globalName(ROUTER_WITH_METADATA_RESOLVER).addProcessorsPart().addIndexPart(0).build();

    MessageMetadataType inputMessageType = MessageMetadataType.builder()
        .payload(typeBuilder.withFormat(JAVA).objectType().build())
        .attributes(typeBuilder.withFormat(JAVA).objectType().build())
        .build();

    MetadataResult<RouterInputMetadataDescriptor> inputMetadataResult =
        metadataService.getRouterInputMetadata(location, null, () -> inputMessageType);

    assertThat(inputMetadataResult.isSuccess(), is(true));
    RouterInputMetadataDescriptor inputMetadata = inputMetadataResult.get();

    MessageMetadataType routeInputType = inputMetadata.getRouteInputMessageTypes().get("metaroute");
    assertThat(routeInputType.getPayloadType().get().getMetadataFormat(), is(JSON));
  }

  @Test
  @Issue("W-16408471")
  public void routerWithOnlyChainInputResolver() {
    location = builderFromStringRepresentation("routerWithOnlyChainInputMetadataResolver/processors/0").build();

    MessageMetadataType inputMessageType = MessageMetadataType.builder()
        .payload(typeBuilder.withFormat(JAVA).objectType().build())
        .attributes(typeBuilder.withFormat(JAVA).objectType().build())
        .build();

    MetadataResult<RouterInputMetadataDescriptor> inputMetadataResult =
        metadataService.getRouterInputMetadata(location, null, () -> inputMessageType);

    assertThat(inputMetadataResult.getFailures(), is(empty()));
    RouterInputMetadataDescriptor inputMetadata = inputMetadataResult.get();

    MessageMetadataType routeInputType = inputMetadata.getRouteInputMessageTypes().get("metaroute");
    assertThat(routeInputType.getPayloadType().get(), is(STRING_TYPE));
  }

  @Test
  @Issue("W-16433612")
  public void routerWithChainInputResolverOnAliasedRoute() {
    location = builderFromStringRepresentation("routerWithChainInputResolverOnAliasedRoute/processors/0").build();

    MessageMetadataType inputMessageType = MessageMetadataType.builder()
        .payload(typeBuilder.withFormat(JAVA).objectType().build())
        .attributes(typeBuilder.withFormat(JAVA).objectType().build())
        .build();

    MetadataResult<RouterInputMetadataDescriptor> inputMetadataResult =
        metadataService.getRouterInputMetadata(location, null, () -> inputMessageType);

    assertThat(inputMetadataResult.getFailures(), is(empty()));
    RouterInputMetadataDescriptor inputMetadata = inputMetadataResult.get();

    assertThat(inputMetadata.getRouteInputMessageTypes().keySet(), containsInAnyOrder("aliasedRoute"));
    MessageMetadataType routeInputType = inputMetadata.getRouteInputMessageTypes().get("aliasedRoute");
    assertThat(routeInputType.getPayloadType().get().getMetadataFormat(), is(JSON));
  }

  @Test
  public void dynamicOutputWithoutContentParam() throws Exception {
    // Resolver for content and output type, no @Content param, resolves only output, with keysResolver and KeyId
    location = Location.builder().globalName(OUTPUT_ONLY_WITHOUT_CONTENT_PARAM).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, VOID_TYPE);
    assertExpectedType(getParameter(typedModel, "type"), STRING_TYPE);

  }

  @Test
  public void dynamicContentWithoutOutput() throws Exception {
    // Resolver for content and output type, no return type, resolves only @Content, with key and KeyId
    location = Location.builder().globalName(CONTENT_ONLY_IGNORES_OUTPUT).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, VOID_TYPE, VOID_TYPE);
    assertExpectedType(getParameter(typedModel, "type"), STRING_TYPE);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void operationOutputWithoutKeyId() throws Exception {
    location = Location.builder().globalName(OUTPUT_METADATA_WITHOUT_KEY_PARAM).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    MetadataType objectMetadataType = (new ParameterTypeWrapper(Object.class, typeLoader)).asMetadataType();

    assertExpectedOutput(typedModel, personType, VOID_TYPE);
    assertExpectedType(getParameter(typedModel, "content"), objectMetadataType, false);

  }

  @Test
  public void contentAndOutputMetadataWithoutKeyId() throws Exception {
    location =
        Location.builder().globalName(CONTENT_AND_OUTPUT_METADATA_WITHOUT_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, VOID_TYPE);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void contentMetadataWithoutKeysWithKeyId() throws Exception {
    location =
        Location.builder().globalName(CONTENT_METADATA_WITHOUT_KEYS_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, VOID_TYPE, VOID_TYPE);
    assertExpectedType(getParameter(typedModel, "type"), STRING_TYPE);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void outputMetadataWithoutKeysWithKeyId() throws Exception {
    location =
        Location.builder().globalName(OUTPUT_METADATA_WITHOUT_KEYS_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, VOID_TYPE);
    assertExpectedType(getParameter(typedModel, "type"), STRING_TYPE);

  }

  @Test
  public void messageAttributesVoidTypeMetadata() throws Exception {
    location = Location.builder().globalName(MESSAGE_ATTRIBUTES_ANY_TYPE_METADATA).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, ANY_TYPE, ANY_TYPE);
    assertExpectedType(getParameter(typedModel, TARGET_PARAMETER_NAME), STRING_TYPE);
  }

  @Test
  public void messageAttributesStringTypeMetadata() throws Exception {
    location = Location.builder().globalName(MESSAGE_ATTRIBUTES_PERSON_TYPE_METADATA).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, types.get(StringAttributes.class.getName()));
    assertExpectedType(getParameter(typedModel, "type"), STRING_TYPE);

  }

  @Test
  public void attributesDynamicPersonTypeMetadata() throws Exception {
    location = Location.builder().globalName(OUTPUT_ATTRIBUTES_WITH_DYNAMIC_METADATA).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    OutputModel attributesOutputModel = typedModel.getOutputAttributes();
    assertThat(attributesOutputModel.hasDynamicType(), is(true));
    MetadataType type = typedModel.getOutputAttributes().getType();
    assertThat(type, is(instanceOf(ObjectType.class)));
    ObjectType dictionary = (ObjectType) type;
    assertThat(dictionary.getOpenRestriction().get(), is(instanceOf(StringType.class)));
  }

  @Test
  public void messageAttributesStringTypeMetadataExplicitResolution() throws Exception {
    location = Location.builder().globalName(OUTPUT_ATTRIBUTES_WITH_DYNAMIC_METADATA).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<OutputMetadataDescriptor> outputMetadataDescriptorResult =
        metadataService.getOutputMetadata(location, PERSON_METADATA_KEY);

    assertThat(outputMetadataDescriptorResult.isSuccess(), is(true));
    OutputMetadataDescriptor outputMetadataDescriptor = outputMetadataDescriptorResult.get();
    TypeMetadataDescriptor attributesMetadata = outputMetadataDescriptor.getAttributesMetadata();
    assertThat(attributesMetadata.isDynamic(), is(true));
    assertThat(attributesMetadata.getType(), is(instanceOf(ObjectType.class)));
    ObjectType dictionary = (ObjectType) attributesMetadata.getType();
    assertThat(dictionary.getOpenRestriction().get(), is(instanceOf(StringType.class)));
  }

  @Test
  public void attributesUnionTypeMetadata() throws Exception {
    location = Location.builder().globalName(OUTPUT_ATTRIBUTES_WITH_DECLARED_SUBTYPES_METADATA).addProcessorsPart()
        .addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel,
                         types.get(Shape.class.getName()), types.get(AbstractOutputAttributes.class.getName()));
  }

  @Test
  public void getContentMetadataWithKey() throws Exception {
    location = Location.builder().globalName(CONTENT_METADATA_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, typeBuilder.anyType().build(), VOID_TYPE);
    assertExpectedType(getParameter(typedModel, "type"), STRING_TYPE);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void getContentMetadataWithoutRequiredKeyId() throws Exception {
    location = Location.builder().globalName(CONTENT_METADATA_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, typeBuilder.anyType().build(), VOID_TYPE);
    assertExpectedType(getParameter(typedModel, "type"), STRING_TYPE);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void getOutputMetadataWithKey() throws Exception {
    location = Location.builder().globalName(OUTPUT_METADATA_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    MetadataType objectMetadataType = (new ParameterTypeWrapper(Object.class, typeLoader)).asMetadataType();

    assertExpectedOutput(typedModel, personType, VOID_TYPE);
    assertExpectedType(getParameter(typedModel, "type"), STRING_TYPE);
    assertExpectedType(getParameter(typedModel, "content"), objectMetadataType, false);
  }

  @Test
  public void dynamicContentWithoutKeyId() throws Exception {
    location = Location.builder().globalName(CONTENT_METADATA_WITHOUT_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, typeBuilder.anyType().build(), VOID_TYPE);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void dynamicOutputWithoutKeyId() throws Exception {
    location = Location.builder().globalName(OUTPUT_METADATA_WITHOUT_KEY_PARAM).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    MetadataType objectMetadataType = (new ParameterTypeWrapper(Object.class, typeLoader)).asMetadataType();

    assertExpectedOutput(typedModel, personType, VOID_TYPE);
    assertExpectedType(getParameter(typedModel, "content"), objectMetadataType, false);
  }

  @Test
  public void dynamicOutputAndContentWithCache() throws Exception {
    location = Location.builder().globalName(CONTENT_AND_OUTPUT_CACHE_RESOLVER).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    MetadataType outputType = typedModel.getOutput().getType();
    MetadataType contentType = getParameter(typedModel, "content").getType();
    assertThat(contentType, is(equalTo(outputType)));
  }

  @Test
  public void resolverContentWithContextClassLoader() throws Exception {
    location = Location.builder().globalName(OUTPUT_METADATA_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    resolveTestWithContextClassLoader(RESOLVER_CONTENT_WITH_CONTEXT_CLASSLOADER,
                                      MetadataExtensionFunctionalTestCase::getSuccessComponentDynamicMetadata);
  }

  @Test
  public void resolverOutputWithContextClassLoader() throws Exception {
    location = Location.builder().globalName(OUTPUT_METADATA_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    resolveTestWithContextClassLoader(RESOLVER_OUTPUT_WITH_CONTEXT_CLASSLOADER,
                                      MetadataExtensionFunctionalTestCase::getSuccessComponentDynamicMetadata);
  }

  @Test
  public void shouldInheritOperationResolvers() throws Exception {
    location =
        Location.builder().globalName(SHOULD_INHERIT_OPERATION_PARENT_RESOLVERS).addProcessorsPart().addIndexPart(0).build();

    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    assertSuccessResult(metadataKeysResult);
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys.size(), is(3));
    assertThat(metadataKeys, hasItems(metadataKeyWithId(PERSON), metadataKeyWithId(CAR), metadataKeyWithId(HOUSE)));

    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, VOID_TYPE);
    assertExpectedType(getParameter(typedModel, "type"), STRING_TYPE);
    // TODO MULE-14190: Revamp MetadataScope annotation
    // assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void pagedOperationMetadataTestCase() throws Exception {
    location = Location.builder().globalName(PAGED_OPERATION_METADATA).addProcessorsPart().addIndexPart(0).build();
    ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "animal"), types.get(Animal.class.getName()));
  }

  @Test
  public void pagedOperationResultMetadataTestCase() throws Exception {
    location = Location.builder().globalName(PAGED_OPERATION_METADATA_RESULT).addProcessorsPart().addIndexPart(0).build();
    ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);
    MetadataType param = metadataDescriptor.getModel().getOutput().getType();
    assertThat(param, is(instanceOf(ArrayType.class)));
    assertThat(getId(param).get(), is(Iterator.class.getName()));
    assertMessageType(((ArrayType) param).getType(), personType, types.get(Animal.class.getName()));
  }

  @Test
  public void pagedOperationResultWithAttributeResolverMetadataTestCase() throws Exception {
    location = Location.builder().globalName(PAGED_OPERATION_METADATA_RESULT_WITH_ATTRIBUTES).addProcessorsPart().addIndexPart(0)
        .build();
    ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);
    MetadataType param = metadataDescriptor.getModel().getOutput().getType();
    assertThat(param, is(instanceOf(ArrayType.class)));
    assertThat(getId(param).get(), is(Iterator.class.getName()));
    assertMessageType(((ArrayType) param).getType(), personType, personType);
  }

  @Test
  public void componentWithStaticInputs() throws IOException {
    location = Location.builder().globalName(TYPE_WITH_DECLARED_SUBTYPES_METADATA).addProcessorsPart().addIndexPart(0).build();
    ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "plainShape"), types.get(Shape.class.getName()));
    assertExpectedType(getParameter(typedModel, "animal"), types.get(Animal.class.getName()));
    assertExpectedType(getParameter(typedModel, "rectangleSubtype"), types.get(Rectangle.class.getName()));
  }

  @Test
  public void retrieveKeysFromBooleanMetadataKey() {
    location = Location.builder().globalName(BOOLEAN_METADATA_KEY).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<MetadataKeysContainer> result = metadataService.getMetadataKeys(location);
    assertSuccessResult(result);
    String booleanMetadataResolver = "BooleanMetadataResolver";
    assertThat(result.get().getCategories(), contains(booleanMetadataResolver));
    Set<MetadataKey> metadataKeys = result.get().getKeys(booleanMetadataResolver).get();
    assertThat(metadataKeys, hasItems(metadataKeyWithId("FALSE"), metadataKeyWithId("TRUE")));
  }

  @Test
  public void booleanMetadataKey() throws IOException {
    location = Location.builder().globalName(BOOLEAN_METADATA_KEY).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result =
        metadataService.getOperationMetadata(location, newKey("true").build());
    assertSuccessResult(result);
    assertExpectedType(getParameter(result.get().getModel(), "content"), types.get(SwordFish.class.getName()), true);
  }

  @Test
  public void retrieveKeysFromEnumMetadataKey() {
    location = Location.builder().globalName(ENUM_METADATA_KEY).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<MetadataKeysContainer> result = metadataService.getMetadataKeys(location);
    assertSuccessResult(result);
    String enumMetadataResolver = "EnumMetadataResolver";
    assertThat(result.get().getCategories(), contains(enumMetadataResolver));

    Set<MetadataKey> metadataKeys = result.get().getKeys(enumMetadataResolver).get();
    MetadataKeyMatcher[] metadataKeyMatchers = Stream.of(AnimalClade.values())
        .map(Object::toString)
        .map(MetadataKeyMatcher::metadataKeyWithId)
        .toArray(MetadataKeyMatcher[]::new);

    assertThat(metadataKeys, hasItems(metadataKeyMatchers));
  }

  @Test
  public void enumMetadataKey() throws IOException {
    location = Location.builder().globalName(ENUM_METADATA_KEY).addProcessorsPart().addIndexPart(0).build();
    ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(newKey("MAMMAL").build());
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "content"), types.get(Bear.class.getName()), true);
  }

  @Test
  public void metadataKeyDefaultValue() throws Exception {
    location = Location.builder().globalName(METADATA_KEY_DEFAULT_VALUE).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(location);
    assertSuccessResult(result);
    MetadataType type = result.get().getModel().getOutput().getType();
    assertThat(type, is(instanceOf(ObjectType.class)));
    ObjectType objectType = (ObjectType) type;
    assertThat(objectType.getFields(), hasSize(2));
    objectType.getFields().forEach(f -> assertThat(f.getKey().getName().getLocalPart(), oneOf(TIRES, BRAND)));
    Optional<MetadataKey> metadataKeyOptional = result.get().getMetadataAttributes().getKey();
    assertThat(metadataKeyOptional.isPresent(), is(true));
    assertThat(metadataKeyOptional.get().getId(), is(CAR));
  }

  @Test
  public void defaultValueMetadataKey() throws Exception {
    location = Location.builder().globalName(METADATA_KEY_DEFAULT_VALUE).addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(location);
    assertSuccessResult(result);
    assertResolvedKey(result, CAR_KEY);
    ComponentMetadataDescriptor<OperationModel> descriptor = result.get();
    MetadataType type = descriptor.getModel().getOutput().getType();
    assertThat(type, is(instanceOf(ObjectType.class)));
    assertThat(((ObjectType) type).getFields(), hasSize(2));
  }

  @Test
  public void operationWithOptionalMetadataKeyIdNotConfigured() throws Exception {
    location = Location.builder().globalName(METADATA_KEY_OPTIONAL).addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(location);
    assertSuccessResult(result);
  }

  @Test
  public void operationWhichReturnsListOfMessages() throws Exception {
    location = Location.builder().globalName("listOfMessages").addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(location);
    assertSuccessResult(result);
    ComponentMetadataDescriptor<OperationModel> descriptor = result.get();
    MetadataType param = descriptor.getModel().getOutput().getType();
    assertThat(param, is(instanceOf(ArrayType.class)));
    assertMessageType(((ArrayType) param).getType(),
                      STRING_TYPE, types.get(StringAttributes.class.getName()));
  }

  @Test
  public void operationWhichReturnsDynamicListOfMessages() throws Exception {
    location = Location.builder().globalName("dynamicListOfMessages").addProcessorsPart().addIndexPart(0).build();
    MetadataType param = getResolvedTypeFromList();
    assertMessageType(((ArrayType) param).getType(), personType, TYPE_BUILDER.anyType().build());
  }

  @Test
  public void operationWhichReturnsDynamicListOfObjects() throws Exception {
    location = Location.builder().globalName("dynamicListOfObjects").addProcessorsPart().addIndexPart(0).build();
    MetadataType param = getResolvedTypeFromList();
    assertExpectedType(((ArrayType) param).getType(), personType);
  }

  @Test
  public void operationReceivesListOfObjects() throws Exception {
    location = Location.builder().globalName("objectListAsInput").addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> operationMetadata =
        metadataService.getOperationMetadata(location);
    MetadataType objects = getParameter(operationMetadata.get().getModel(), "objects").getType();

    assertThat(objects, is(instanceOf(ArrayType.class)));
    assertThat(((ArrayType) objects).getType(), is(personType));
  }

  @Test
  public void operationReceivesNullTypeOfList() throws Exception {
    location = Location.builder().globalName("nullListAsInput").addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> operationMetadata =
        metadataService.getOperationMetadata(location);
    MetadataType objects = getParameter(operationMetadata.get().getModel(), "objects").getType();

    assertThat(objects, is(instanceOf(NullType.class)));
  }

  @Test
  public void operationReceivesExclusiveOptionalParameterGroup() throws Exception {
    location =
        Location.builder().globalName("inputHasExclusiveOptionalParameterGroup").addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> operationMetadata =
        metadataService.getOperationMetadata(location);
    ParameterGroupModel dessert = getParameterGroup(operationMetadata.get().getModel(), "dessert");

    assertThat(dessert.getName(), is("dessert"));
  }

  @Test
  public void operationReceivesPojoWithExclusiveOptionalParameterGroup() throws Exception {
    location = Location.builder().globalName("inputHasPojoWithExclusiveOptionalParameterGroup").addProcessorsPart()
        .addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> operationMetadata =
        metadataService.getOperationMetadata(location);
    ParameterModel dessertOrder = getParameter(operationMetadata.get().getModel(), "dessertOrder");

    assertThat(dessertOrder.getName(), is("dessertOrder"));
  }

  @Test
  public void partialMultiLevelKeyShowInDslResolver() throws Exception {
    location = Location.builder().globalName("partialMultiLevelKeyShowInDslResolver").addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    assertSuccessResult(metadataKeysResult);
    final Set<MetadataKey> continents = getKeysFromContainer(metadataKeysResult.get());
    assertThat(continents, hasSize(1));

    assertThat(continents, hasItem(metadataKeyWithId(AMERICA).withDisplayName(AMERICA).withPartName(CONTINENT)));
    assertThat(continents, not(hasItem(metadataKeyWithId(EUROPE).withDisplayName(EUROPE).withPartName(CONTINENT))));
  }

  @Test
  public void outputMetadataWithoutKeysWithKeyIdWithConfig() throws Exception {
    location =
        Location.builder().globalName(OUTPUT_METADATA_WITH_KEY_ID_USING_CONFIG).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, VOID_TYPE);
    assertExpectedType(getParameter(typedModel, "type"), STRING_TYPE);

  }

  private MetadataType getResolvedTypeFromList() {
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(location);
    assertSuccessResult(result);
    ComponentMetadataDescriptor<OperationModel> descriptor = result.get();
    MetadataType param = descriptor.getModel().getOutput().getType();
    assertThat(param, is(instanceOf(ArrayType.class)));
    assertThat(getId(param).get(), is(List.class.getName()));
    return param;
  }

  /**
   * Test template that sets an "invalid" classloader in TCCL different from the one that was used to register the extension and
   * asserts that, it sets back the original classloader to TCCL. Done in this way due to it is not possible to change extension
   * model classloader property once it is registered.
   */
  private void resolveTestWithContextClassLoader(String flowName, Callback<MetadataOperationTestCase> doAction)
      throws Exception {
    location = Location.builder().globalName(flowName).addProcessorsPart().addIndexPart(0).build();
    TestThreadContextClassLoaderResolver.reset();
    final ClassLoader originalClassLoader = org.mule.test.metadata.extension.MetadataConnection.class.getClassLoader();
    withContextClassLoader(mock(ClassLoader.class), () -> {
      doAction.execute(MetadataOperationTestCase.this);
      assertThat(TestThreadContextClassLoaderResolver.getCurrentState(), is(sameInstance(originalClassLoader)));
    });
  }

  private ParameterModel getParameter(ComponentModel model, String parameterName) {
    return model.getAllParameterModels().stream()
        .filter(p -> p.getName().equals(parameterName)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Parameter not found"));
  }

  private ParameterGroupModel getParameterGroup(ComponentModel model, String parameterName) {
    return model.getParameterGroupModels().stream()
        .filter(p -> p.getName().equals(parameterName)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Parameter Group not found"));
  }

  private static class TestScopeOutputMetadataContext implements ScopeOutputMetadataContext {

    private final MessageMetadataType innerChainOutputMessage;

    private TestScopeOutputMetadataContext(MessageMetadataType innerChainOutputMessage) {
      this.innerChainOutputMessage = innerChainOutputMessage;
    }

    @Override
    public Supplier<MessageMetadataType> getInnerChainOutputMessageType() {
      return () -> innerChainOutputMessage;
    }

    @Override
    public Supplier<MessageMetadataType> getScopeInputMessageType() {
      // Not needed for this test
      return () -> null;
    }
  }

  private static class TestRouterOutputMetadataContext implements RouterOutputMetadataContext {

    private final Map<String, Supplier<MessageMetadataType>> routeOutputMessageTypes;

    private TestRouterOutputMetadataContext(Map<String, Supplier<MessageMetadataType>> routeOutputMessageTypes) {
      this.routeOutputMessageTypes = routeOutputMessageTypes;
    }

    @Override
    public Map<String, Supplier<MessageMetadataType>> getRouteOutputMessageTypes() {
      return routeOutputMessageTypes;
    }

    @Override
    public Supplier<MessageMetadataType> getRouterInputMessageType() {
      // Not needed for this test
      return () -> null;
    }
  }
}
