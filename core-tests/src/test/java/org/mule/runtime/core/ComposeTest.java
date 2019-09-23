/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import org.junit.Test;
import reactor.core.publisher.Mono;

public class ComposeTest {


  @Test
  public void test() {

    Mono<String> mono1 = Mono.create(sink -> {
      System.out.println("doing 1");
      sink.error(new RuntimeException("Mono 1"));
    });

    Mono<String> mono2 = Mono.create(sink -> sink.success("Mono 2"));

    Mono<String> composed = mono1.compose(v -> mono2);
    String value = composed.block();

    System.out.println(value);
  }
}
