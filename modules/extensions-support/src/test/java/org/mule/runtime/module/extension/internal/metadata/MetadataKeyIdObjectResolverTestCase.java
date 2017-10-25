/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.api.metadata.MultilevelMetadataKeyBuilder.newKey;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.extension.api.dsql.DsqlQuery;
import org.mule.runtime.extension.api.dsql.QueryTranslator;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.QueryParameterModelProperty;
import org.mule.test.metadata.extension.LocationKey;

import java.lang.reflect.Field;
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
  private static final MetadataKey MULTILEVEL_KEY = newKey(AMERICA, CONTINENT).withChild(newKey(USA, COUNTRY)
      .withChild(newKey(SFO, CITY))).build();
  private static final MetadataKey INCOMPLETE_MULTILEVEL_KEY = newKey(AMERICA, CONTINENT).withChild(newKey(USA, COUNTRY)).build();
  private static final String OPERATION_NAME = "SomeOperation";
  private static final String EMPTY = "";
  private MetadataKeyIdObjectResolver keyIdObjectResolver;

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
    mockMetadataKeyModelProp(continentParam, 1);
    mockMetadataKeyModelProp(countryParam, 2);
    mockMetadataKeyModelProp(cityParam, 3);

    mockDeclaringMemberModelProp(continentParam, CONTINENT);
    mockDeclaringMemberModelProp(countryParam, COUNTRY);
    mockDeclaringMemberModelProp(cityParam, CITY);

    mockQueryModelProp(continentParam);
    mockQueryModelProp(countryParam);
    mockQueryModelProp(cityParam);

    when(componentModel.getName()).thenReturn(OPERATION_NAME);
  }

  private void mockMetadataKeyModelProp(ParameterModel param, int pos) {
    when(param.getModelProperty(MetadataKeyPartModelProperty.class)).thenReturn(of(new MetadataKeyPartModelProperty(pos)));
  }

  private void mockDeclaringMemberModelProp(ParameterModel param, String name) {
    Field f = getField(LocationKey.class, name).get();
    when(param.getModelProperty(DeclaringMemberModelProperty.class)).thenReturn(of(new DeclaringMemberModelProperty(f)));
  }

  private void mockQueryModelProp(ParameterModel param) {
    when(param.getModelProperty(QueryParameterModelProperty.class)).thenReturn(empty());
  }

  @Test
  public void resolveSingleLevelKey() throws MetadataResolvingException {
    setParameters(continentParam);
    setMetadataKeyIdModelProperty(String.class);
    keyIdObjectResolver = new MetadataKeyIdObjectResolver(componentModel);
    final Object key = keyIdObjectResolver.resolve(newKey(AMERICA, CONTINENT).build());
    assertThat(key, is(instanceOf(String.class)));
    String stringKey = (String) key;
    assertThat(stringKey, is(AMERICA));
  }

  @Test
  public void resolveMultiLevelKey() throws MetadataResolvingException {
    setParameters(continentParam, countryParam, cityParam);
    setMetadataKeyIdModelProperty(LocationKey.class);

    keyIdObjectResolver = new MetadataKeyIdObjectResolver(componentModel);
    final Object key = keyIdObjectResolver.resolve(MULTILEVEL_KEY);
    assertThat(key, is(instanceOf(LocationKey.class)));
    LocationKey locationKey = (LocationKey) key;
    assertThat(locationKey, hasProperty(CONTINENT, is(AMERICA)));
    assertThat(locationKey, hasProperty(COUNTRY, is(USA)));
    assertThat(locationKey, hasProperty(CITY, is(SFO)));
  }

  @Test
  public void resolveDefaultMultiLevelKey() throws MetadataResolvingException {
    setParameters(continentParam, countryParam, cityParam);
    setMetadataKeyIdModelProperty(LocationKey.class);
    when(continentParam.getDefaultValue()).thenReturn(AMERICA);
    when(countryParam.getDefaultValue()).thenReturn(USA);
    when(cityParam.getDefaultValue()).thenReturn(SFO);

    keyIdObjectResolver = new MetadataKeyIdObjectResolver(componentModel);
    final Object key = keyIdObjectResolver.resolve();
    assertThat(key, is(instanceOf(LocationKey.class)));
    LocationKey locationKey = (LocationKey) key;
    assertThat(locationKey, hasProperty(CONTINENT, is(AMERICA)));
    assertThat(locationKey, hasProperty(COUNTRY, is(USA)));
    assertThat(locationKey, hasProperty(CITY, is(SFO)));
  }

  @Test
  public void resolveDefaultSingleKey() throws MetadataResolvingException {
    setParameters(continentParam);
    setMetadataKeyIdModelProperty(String.class);
    when(continentParam.getDefaultValue()).thenReturn(AMERICA);
    keyIdObjectResolver = new MetadataKeyIdObjectResolver(componentModel);
    final Object key = keyIdObjectResolver.resolve();
    assertThat(key, is(instanceOf(String.class)));
    assertThat(key, is(AMERICA));
  }

  @Test
  public void resolveNoKeyParam() throws MetadataResolvingException {
    keyIdObjectResolver = new MetadataKeyIdObjectResolver(componentModel);
    final Object key = keyIdObjectResolver.resolve();
    assertThat(key, is(instanceOf(String.class)));
    assertThat(key, is(""));
  }

  @Test
  public void resolveDsql() throws MetadataResolvingException {
    ParameterModel queryParam = mock(ParameterModel.class);
    when(queryParam.getModelProperty(QueryParameterModelProperty.class))
        .thenReturn(Optional.of(new QueryParameterModelProperty(QueryTranslator.class)));
    mockMetadataKeyModelProp(queryParam, 1);
    setParameters(queryParam);
    setMetadataKeyIdModelProperty(String.class);

    MetadataKey dsqlKey = newKey("dsql:SELECT id FROM Circle WHERE (diameter < 18)").build();
    keyIdObjectResolver = new MetadataKeyIdObjectResolver(componentModel);
    final Object resolvedKey = keyIdObjectResolver.resolve(dsqlKey);
    assertThat(resolvedKey, is(instanceOf(DsqlQuery.class)));
    DsqlQuery query = (DsqlQuery) resolvedKey;
    assertThat(query.getFields(), hasSize(1));
    assertThat(query.getType().getName(), is("Circle"));
  }

  @Test
  public void failToResolveWithNotInstantiableKey() throws MetadataResolvingException {
    exception.expect(MetadataResolvingException.class);
    exception.expectMessage(
                            is("MetadataKey object of type 'NotInstantiableClass' from the component 'SomeOperation' could not be instantiated"));
    exception.expectCause(is(instanceOf(IllegalArgumentException.class)));

    setParameters(continentParam, countryParam, cityParam);
    setMetadataKeyIdModelProperty(NotInstantiableClass.class);

    keyIdObjectResolver = new MetadataKeyIdObjectResolver(componentModel);
    keyIdObjectResolver.resolve(MULTILEVEL_KEY);
  }

  @Test
  public void failToResolveWithMissingLevels() throws MetadataResolvingException {
    exception.expect(MetadataResolvingException.class);
    exception.expectMessage(is("The given MetadataKey does not provide all the required levels. Missing levels: [city]"));

    setParameters(continentParam, countryParam, cityParam);
    setMetadataKeyIdModelProperty(LocationKey.class);

    keyIdObjectResolver = new MetadataKeyIdObjectResolver(componentModel);
    keyIdObjectResolver.resolve(INCOMPLETE_MULTILEVEL_KEY);
  }

  @Test
  public void failToResolveWithOutMetadataKeyId() throws MetadataResolvingException {
    exception.expect(MetadataResolvingException.class);
    exception.expectMessage(is("Component 'SomeOperation' doesn't have a MetadataKeyId parameter associated"));

    setParameters(continentParam, countryParam, cityParam);
    when(componentModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(empty());

    keyIdObjectResolver = new MetadataKeyIdObjectResolver(componentModel);
    keyIdObjectResolver.resolve(MULTILEVEL_KEY);
  }

  @Test
  public void failToResolveWithNoDefaultValues() throws MetadataResolvingException {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("does not have a default value for all it's components."));

    setParameters(continentParam, countryParam, cityParam);
    setMetadataKeyIdModelProperty(LocationKey.class);
    when(continentParam.getDefaultValue()).thenReturn(AMERICA);
    when(cityParam.getDefaultValue()).thenReturn(SFO);

    keyIdObjectResolver = new MetadataKeyIdObjectResolver(componentModel);
    keyIdObjectResolver.resolve();
  }

  @Test
  public void failToResolveWithMultipleChildren() throws MetadataResolvingException {
    exception.expect(MetadataResolvingException.class);
    exception
        .expectMessage(
                       is("MetadataKey used for Metadata resolution must only have one child per level. Key 'USA' has [SFO, NY] as children."));

    final MetadataKey invalidMetadataKey = newKey(AMERICA, CONTINENT)
        .withChild(newKey(USA, COUNTRY).withChild(newKey(SFO, CITY)).withChild(newKey(NY, CITY))).build();
    setParameters(continentParam, countryParam, cityParam);
    setMetadataKeyIdModelProperty(LocationKey.class);

    keyIdObjectResolver = new MetadataKeyIdObjectResolver(componentModel);
    keyIdObjectResolver.resolve(invalidMetadataKey);
  }

  public void setParameters(ParameterModel... parameterModels) {
    when(componentModel.getAllParameterModels()).thenReturn(asList(parameterModels));
  }

  private void setMetadataKeyIdModelProperty(Class<?> type) {
    when(componentModel.getModelProperty(MetadataKeyIdModelProperty.class))
        .thenReturn(of(new MetadataKeyIdModelProperty(new JavaTypeLoader(this.getClass().getClassLoader()).load(type), EMPTY)));
  }

  private class NotInstantiableClass {

  }
}
