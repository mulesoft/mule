/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.PersonalInfo;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class GroupValueSetterTestCase extends AbstractMuleTestCase {

  private static final String NAME = "name";
  private static final Integer AGE = 50;
  private static final Date DATE = new Date();


  private ValueSetter valueSetter;

  @Mock
  private ResolverSetResult result;

  @Before
  public void before() throws Exception {
    final String personalInfo = "personalInfo";
    ParameterGroupDescriptor group =
        new ParameterGroupDescriptor("group", new TypeWrapper(PersonalInfo.class),
                                     ExtensionsTypeLoaderFactory.getDefault().createTypeLoader().load(PersonalInfo.class),
                                     getField(HeisenbergExtension.class, personalInfo).get());

    Map<String, Object> resultMap = new HashMap<>();
    resultMap.put("name", NAME);
    resultMap.put("age", AGE);
    resultMap.put("dateOfBirth", DATE);

    when(result.asMap()).thenReturn(resultMap);

    valueSetter = new GroupValueSetter(group);
  }

  @Test
  public void set() throws Exception {
    HeisenbergExtension extension = new HeisenbergExtension();
    valueSetter.set(extension, result);

    assertThat(extension.getPersonalInfo().getName(), is(NAME));
    assertThat(extension.getPersonalInfo().getAge(), is(AGE));
    assertThat(extension.getPersonalInfo().getDateOfBirth(), is(sameInstance(DATE)));
  }
}
