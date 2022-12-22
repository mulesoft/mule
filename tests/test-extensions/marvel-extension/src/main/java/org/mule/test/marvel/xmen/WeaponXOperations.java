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
import static org.mule.runtime.api.util.IOUtils.toByteArray;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
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

  public Iterator<String> wolverineBlocklist() {
    return asList("Sabretooth", "Omega Red", "Dr. Cornelius", "Lady Deathstrike", "Mystique", "Silver Samurai").iterator();
  }

  public void wolverineShred(@Content @Optional(defaultValue = "#[payload]") List enemies) {
    for (Object enemy : enemies) {
      // Operation too violent to implement explicitly
    }
  }

  public void wolverineChillOut(@ParameterGroup(name = "forest", showInDsl = true) CanadianForest forest) {
    // Wolverine takes a vacation back home before the next mission
    wolverineChillOutQuick(forest);
  }

  public void wolverineChillOutQuick(@ParameterGroup(name = "forest") CanadianForest forest) {
    // Wolverine takes a short vacation back home before the next mission
    for (String bear : forest.getBears()) {
      // He pets them, what did you expect wolverine to do?
    }
    toByteArray((InputStream) forest.getRiver().getValue());
    for (String friends : forest.getFriends()) {
      // some people to share some drinks with
    }
  }

  public void gambitReluctantCharge(@Content @Optional InputStream input) {
    // do nothing
  }

  public void gambitChargeItems(@Content @Optional(defaultValue = "#[payload]") TypedValue<Iterator> items) {
    final Iterator iter = items.getValue();
    while (iter.hasNext()) {
      Object value = iter.next();
    }
  }

  public void gambitReluctantChargeItems(@Content @Optional TypedValue<Iterator> items) {
    // do nothing
  }

  /**
   *
   * @param wounds
   * @return
   */
  @OutputResolver(output = PassthroughMetadataResolver.class)
  public Result<Object, Void> woundsPassthrough(@Content(primary = true) TypedValue<Object> wounds) {
    Result.Builder<Object, Void> builder = Result.<Object, Void>builder()
        .output(wounds.getValue())
        .mediaType(wounds.getDataType().getMediaType());

    return builder.build();
  }

  public static class PassthroughMetadataResolver implements OutputTypeResolver<Object> {

    private static final AnyType ANY_TYPE = BaseTypeBuilder.create(MetadataFormat.JAVA).anyType().build();

    @Override
    public String getCategoryName() {
      return "HttpPolicyTransform";
    }

    @Override
    public MetadataType getOutputType(MetadataContext context, Object key) {
      return ANY_TYPE;
    }
  }
}
