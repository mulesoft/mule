/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.metadata.PartAwareMetadataKeyBuilder.newKey;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyPartModelProperty;
import org.mule.test.metadata.extension.LocationKey;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MetadataKeyIdObjectResolverTestCase {

  private static final String CITY = "city";
  private static final String SFO = "SFO";
  private static final String NY = "NY";
  private static final String COUNTRY = "country";
  private static final String USA = "USA";
  private static final String CONTINENT = "continent";
  private static final String AMERICA = "AMERICA";
  private static final MetadataKey MULTILEVEL_KEY =
      newKey(AMERICA, CONTINENT).withChild(newKey(USA, COUNTRY).withChild(newKey(SFO, CITY))).build();
  private static final MetadataKey INCOMPLETE_MULTILEVEL_KEY = newKey(AMERICA, CONTINENT).withChild(newKey(USA, COUNTRY)).build();
  private static final String OPERATION_NAME = "SomeOperation";
  private final MetadataKeyIdObjectResolver keyIdObjectResolver = new MetadataKeyIdObjectResolver();

  @Mock
  public ComponentModel componentModel;

  @Mock
  public ParameterModel continentParam;

  @Mock
  public ParameterModel countryParam;

  @Mock
  public ParameterModel cityParam;

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    when(continentParam.getModelProperty(MetadataKeyPartModelProperty.class))
        .thenReturn(Optional.of(new MetadataKeyPartModelProperty(1)));
    when(countryParam.getModelProperty(MetadataKeyPartModelProperty.class))
        .thenReturn(Optional.of(new MetadataKeyPartModelProperty(2)));
    when(cityParam.getModelProperty(MetadataKeyPartModelProperty.class))
        .thenReturn(Optional.of(new MetadataKeyPartModelProperty(3)));
    when(componentModel.getName()).thenReturn(OPERATION_NAME);
  }

  @Test
  public void resolveSingleLevelKey() throws MetadataResolvingException {
    setParameters(continentParam);
    setMetadataKeyIdModelProperty(String.class);
    final Object key = keyIdObjectResolver.resolve(componentModel, newKey(AMERICA, CONTINENT).build());
    assertThat(key, is(instanceOf(String.class)));
    String stringKey = (String) key;
    assertThat(stringKey, is(AMERICA));
  }

  @Test
  public void resolveMultiLevelKey() throws MetadataResolvingException {
    setParameters(continentParam, countryParam, cityParam);
    setMetadataKeyIdModelProperty(LocationKey.class);

    final Object key = keyIdObjectResolver.resolve(componentModel, MULTILEVEL_KEY);
    assertThat(key, is(instanceOf(LocationKey.class)));
    LocationKey locationKey = (LocationKey) key;
    assertThat(locationKey, hasProperty(CONTINENT, is(AMERICA)));
    assertThat(locationKey, hasProperty(COUNTRY, is(USA)));
    assertThat(locationKey, hasProperty(CITY, is(SFO)));
  }

  @Test
  public void failToResolveWithNotInstantiableKey() throws MetadataResolvingException {
    exception.expect(MetadataResolvingException.class);
    exception
        .expectMessage(is("MetadataKey object of type 'NotInstantiableClass' from the component 'SomeOperation' could not be instantiated"));
    exception.expectCause(is(instanceOf(NoSuchMethodException.class)));

    setParameters(continentParam, countryParam, cityParam);
    setMetadataKeyIdModelProperty(NotInstantiableClass.class);

    keyIdObjectResolver.resolve(componentModel, MULTILEVEL_KEY);
  }

  @Test
  public void failToResolveWithMissingLevels() throws MetadataResolvingException {
    exception.expect(MetadataResolvingException.class);
    exception.expectMessage(is("The given MetadataKey does not provide all the required levels. Missing levels: [city]"));

    setParameters(continentParam, countryParam, cityParam);
    setMetadataKeyIdModelProperty(LocationKey.class);

    keyIdObjectResolver.resolve(componentModel, INCOMPLETE_MULTILEVEL_KEY);
  }

  @Test
  public void failToResolveWithOutMetadataKeyId() throws MetadataResolvingException {
    exception.expect(MetadataResolvingException.class);
    exception.expectMessage(is("Component 'SomeOperation' doesn't have a MetadataKeyId parameter associated"));

    setParameters(continentParam, countryParam, cityParam);
    when(componentModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(Optional.empty());

    keyIdObjectResolver.resolve(componentModel, MULTILEVEL_KEY);
  }

  @Test
  public void failToResolveWithMultipleChildren() throws MetadataResolvingException {
    exception.expect(MetadataResolvingException.class);
    exception
        .expectMessage(is("MetadataKey used for Metadata resolution must only have one child per level. Key 'USA' has [SFO, NY] as children."));

    final MetadataKey invalidMetadataKey = newKey(AMERICA, CONTINENT)
        .withChild(newKey(USA, COUNTRY).withChild(newKey(SFO, CITY)).withChild(newKey(NY, CITY))).build();
    setParameters(continentParam, countryParam, cityParam);
    setMetadataKeyIdModelProperty(LocationKey.class);

    keyIdObjectResolver.resolve(componentModel, invalidMetadataKey);
  }

  @Test
  public void failToResolveWithInvalidKeyIdParam() throws MetadataResolvingException {
    exception.expect(MetadataResolvingException.class);
    exception
        .expectMessage(is("'Boolean' type is invalid for MetadataKeyId parameters, use String type instead. Affecting component: 'SomeOperation'"));

    setParameters(continentParam);
    setMetadataKeyIdModelProperty(Boolean.class);

    keyIdObjectResolver.resolve(componentModel, newKey("true", "booleanParam").build());
  }

  public void setParameters(ParameterModel... parameterModels) {
    when(componentModel.getParameterModels()).thenReturn(Arrays.asList(parameterModels));
  }

  private void setMetadataKeyIdModelProperty(Class<?> type) {
    when(componentModel.getModelProperty(MetadataKeyIdModelProperty.class))
        .thenReturn(Optional.of(new MetadataKeyIdModelProperty(new JavaTypeLoader(this.getClass().getClassLoader()).load(type))));
  }

  private class NotInstantiableClass {

  }
}
