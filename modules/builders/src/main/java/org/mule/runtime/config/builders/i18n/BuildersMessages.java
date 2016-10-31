/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.builders.i18n;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

public class BuildersMessages extends I18nMessageFactory {

  private static final BuildersMessages factory = new BuildersMessages();

  private static final String BUNDLE_PATH = getBundlePath("builders");

  public static I18nMessage failedToParseConfigResource(String description) {
    return factory.createMessage(BUNDLE_PATH, 1, description);
  }

  public static I18nMessage propertyTemplateMalformed(String string) {
    return factory.createMessage(BUNDLE_PATH, 2, string);
  }

  public static I18nMessage systemPropertyNotSet(String property) {
    return factory.createMessage(BUNDLE_PATH, 3, property);
  }

  public static I18nMessage mustSpecifyContainerRefOrClassAttribute(String containerAttrib, String refAttrib, String config) {
    return factory.createMessage(BUNDLE_PATH, 4, containerAttrib, refAttrib, config);
  }
}


