/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.Field;

import io.qameta.allure.Issue;
import org.junit.Test;

public class FieldWrapperTestCase extends AbstractMuleTestCase {


  @Test
  @Issue("W-14258904")
  public void notEagerlyAccessible() throws Exception {
    JavaTypeLoader typeLoader = new JavaTypeLoader(getClass().getClassLoader());
    TypeWrapper type = new TypeWrapper(FieldPojo.class, typeLoader);
    FieldElement fieldElement = type.getFields().get(0);
    Field field = fieldElement.getField().get();

    assertThat(field.getName(), equalTo("stringField"));
    assertThat(field.isAccessible(), is(false));

    FieldPojo pojo = new FieldPojo();
    final String hello = "Hello World!";
    field.set(pojo, hello);

    assertThat(pojo.stringField, is(hello));
  }


  public static class FieldPojo {

    private String stringField;
  }

}
