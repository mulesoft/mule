/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.xmen;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.input.ReaderInputStream;

public class WeaponXOperations {

  public List<Result<InputStream, Void>> adamantiumInjectors(int injectorsToCreate, int injectorLoad) {
    final List<Result<InputStream, Void>> injectors = new ArrayList<>();

    for (int i = 0; i < injectorsToCreate; ++i) {
      injectors.add(Result.<InputStream, Void>builder()
          .output(new ReaderInputStream(new StringReader(randomAlphanumeric(injectorLoad)), UTF_8))
          .build());
    }

    return injectors;
  }

  public Iterator<String> wolverineBlacklist() {
    return asList("Sabretooth", "Omega Red", "Dr. Cornelius", "Lady Deathstrike", "Mystique", "Silver Samurai").iterator();
  }
}
