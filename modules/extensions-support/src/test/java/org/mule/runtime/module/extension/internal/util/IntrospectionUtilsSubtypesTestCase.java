/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.SubTypesModel;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IntrospectionUtilsSubtypesTestCase {

  @Mock
  ExtensionModel extensionModel;

  private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Test
  public void getClassesFromSubTypeImplementations() {
    Set<MetadataType> subTypes = ImmutableSet.of(typeLoader.load(AA.class), typeLoader.load(AB.class));
    SubTypesModel subTypesModel = new SubTypesModel(typeLoader.load(A.class), subTypes);

    when(extensionModel.getSubTypes()).thenReturn(ImmutableSet.of(subTypesModel));

    Set<Class<?>> subtypeClasses =
        IntrospectionUtils.getSubtypeClasses(extensionModel, Thread.currentThread().getContextClassLoader());
    assertThat(subtypeClasses, hasItems(A.class, AA.class, AB.class, SomeEnum.class));
  }

  public interface A {

  }

  public class AA implements A {

  }

  public class AB implements A {

    @Parameter
    SomeEnum param;

  }

  public enum SomeEnum {
    A, B, C
  }

}
