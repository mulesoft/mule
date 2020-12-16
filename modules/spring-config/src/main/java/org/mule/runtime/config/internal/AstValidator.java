/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.ValidationResult;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.Collection;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.ast.api.util.MuleAstUtils.validate;

public class AstValidator {

  private static String compToLoc(ComponentAst component) {
    return "[" + component.getMetadata().getFileName().orElse("unknown") + ":"
        + component.getMetadata().getStartLine().orElse(-1) + "]";
  }

  public static ArtifactAst validateAst(ArtifactAst appModel) {
    final ValidationResult validation = validate(appModel);

    final Collection<ValidationResultItem> items = validation.getItems();
    if (!items.isEmpty()) {

      final String allMessages = validation.getItems()
          .stream()
          .map(v -> compToLoc(v.getComponent()) + ": " + v.getMessage())
          .collect(joining(lineSeparator()));


      throw new MuleRuntimeException(createStaticMessage(allMessages));
    }
    return appModel;
  }
}
