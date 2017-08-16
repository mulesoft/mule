/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

import org.mule.tck.junit4.AbstractMuleTestCase;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AbstractAnnotationProcessorTestCase extends AbstractMuleTestCase {

  protected Iterable<JavaFileObject> testSourceFiles() throws Exception {
    // this will be xxx/target/test-classes
    File folder = new File(getClass().getClassLoader().getResource("").getPath().toString());
    // up to levels
    folder = folder.getParentFile().getParentFile();
    folder = new File(folder, getSourceFilesLocation());
    File[] files = folder.listFiles((dir, name) -> name.endsWith(".java"));
    assertThat(files, is(notNullValue()));
    List<JavaFileObject> javaFileObjects = new ArrayList<>(files.length);
    for (File file : files) {
      javaFileObjects.add(JavaFileObjects.forResource(file.toURI().toURL()));
    }
    return javaFileObjects;
  }

  protected String getSourceFilesLocation() {
    return "src/test/java/org/mule/runtime/module/extension/internal/capability/xml/extension";
  }
}
