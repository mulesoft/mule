/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.config.dsl.object.ParsingDelegate;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Utility class containing methods commonly used when parsing an extension.
 *
 * @since 4.2
 */
public class ExtensionParsingUtils {

  static final String CHILD_ELEMENT_KEY_PREFIX = "<<";
  static final String CHILD_ELEMENT_KEY_SUFFIX = ">>";

  public static String getChildKey(String key) {
    return format("%s%s%s", CHILD_ELEMENT_KEY_PREFIX, key, CHILD_ELEMENT_KEY_SUFFIX);
  }

  public static boolean isChildKey(String key) {
    return key.startsWith(CHILD_ELEMENT_KEY_PREFIX) && key.endsWith(CHILD_ELEMENT_KEY_SUFFIX);
  }

  public static String unwrapChildKey(String key) {
    return key.replaceAll(CHILD_ELEMENT_KEY_PREFIX, "").replaceAll(CHILD_ELEMENT_KEY_SUFFIX, "");
  }

  public static <M extends MetadataType, T> Optional<ParsingDelegate<M, T>> locateParsingDelegate(
                                                                                                  List<? extends ParsingDelegate<M, T>> delegatesList,
                                                                                                  M metadataType) {
    return (Optional<ParsingDelegate<M, T>>) delegatesList.stream().filter(candidate -> candidate.accepts(metadataType))
        .findFirst();
  }

  public static boolean acceptsReferences(ParameterModel parameterModel) {
    return parameterModel.getDslConfiguration().allowsReferences();
  }
}
