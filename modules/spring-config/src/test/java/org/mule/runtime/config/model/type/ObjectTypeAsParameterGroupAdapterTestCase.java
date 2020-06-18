/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.model.type;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.config.internal.model.type.MetadataTypeModelAdapter.createParameterizedTypeModelAdapter;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ParameterAst.PARAMETER_AST;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectKeyType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper;
import org.mule.runtime.config.internal.model.type.MetadataTypeModelAdapter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Feature(ARTIFACT_AST)
@Story(PARAMETER_AST)
public class ObjectTypeAsParameterGroupAdapterTestCase extends AbstractMuleTestCase {

  private final static String OBJECT_FIELD_A = "A";
  private final static String OBJECT_FIELD_B = "B";
  private final static String OBJECT_FIELD_C = "C";

  private ObjectType type;
  private ExtensionModelHelper extensionModelHelper;

  @Before
  public void setup() {
    type = createObjectType();
    extensionModelHelper = mock(ExtensionModelHelper.class);
  }

  private ObjectType createObjectType() {
    ObjectType objectType = mock(ObjectType.class);
    Collection<ObjectFieldType> fields = new HashSet<>();
    fields.add(createObjectFieldType(OBJECT_FIELD_C));
    fields.add(createObjectFieldType(OBJECT_FIELD_B));
    fields.add(createObjectFieldType(OBJECT_FIELD_A));
    when(objectType.getFields()).thenReturn(fields);
    return objectType;
  }

  @Test
  public void getParametersSortedByName() {
    MetadataTypeModelAdapter metadataTypeModelAdapter = createParameterizedTypeModelAdapter(type, extensionModelHelper);

    List<ParameterGroupModel> parameterGroupModels = metadataTypeModelAdapter.getParameterGroupModels();
    assertThat(parameterGroupModels, not(empty()));
    assertThat(parameterGroupModels.size(), is(1));

    List<ParameterModel> parameterModels = parameterGroupModels.get(0).getParameterModels();
    assertThat(parameterModels, not(empty()));
    assertThat(parameterModels.size(), is(4));

    List<String> parameters = parameterModels.stream().map(NamedObject::getName).collect(toList());
    assertThat(parameters, is(asList(OBJECT_FIELD_A, OBJECT_FIELD_B, OBJECT_FIELD_C, "name")));
  }

  private ObjectFieldType createObjectFieldType(String name) {
    QName qName = mock(QName.class);
    when(qName.getLocalPart()).thenReturn(name);

    ObjectKeyType objectKeyType = mock(ObjectKeyType.class);
    when(objectKeyType.getName()).thenReturn(qName);

    ObjectFieldType objectFieldType = mock(ObjectFieldType.class);
    when(objectFieldType.getKey()).thenReturn(objectKeyType);
    return objectFieldType;
  }
}
