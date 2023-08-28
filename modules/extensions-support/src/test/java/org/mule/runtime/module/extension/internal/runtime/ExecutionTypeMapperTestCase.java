/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.BLOCKING;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_INTENSIVE;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_LITE;
import static org.mule.runtime.module.extension.internal.runtime.ExecutionTypeMapper.asProcessingType;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collection;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class ExecutionTypeMapperTestCase extends AbstractMuleTestCase {

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Stream.of(ExecutionType.values())
        .map(type -> new Object[] {type})
        .collect(toList());
  }

  private final ExecutionType type;

  public ExecutionTypeMapperTestCase(ExecutionType type) {
    this.type = type;
  }

  @Test
  public void map() {
    if (type == CPU_INTENSIVE) {
      assertMap(ProcessingType.CPU_INTENSIVE);
    } else if (type == CPU_LITE) {
      assertMap(ProcessingType.CPU_LITE);
    } else if (type == BLOCKING) {
      assertMap(ProcessingType.BLOCKING);
    } else {
      fail("Unsupported type: " + type);
    }
  }

  private void assertMap(ProcessingType expected) {
    assertThat(asProcessingType(type), is(expected));
  }
}
