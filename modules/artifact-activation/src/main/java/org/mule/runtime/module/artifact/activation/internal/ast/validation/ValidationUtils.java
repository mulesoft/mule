package org.mule.runtime.module.artifact.activation.internal.ast.validation;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import org.mule.runtime.ast.api.validation.ArtifactAstValidator;
import org.mule.runtime.ast.api.validation.ValidationResult;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.core.api.config.ConfigurationException;

import java.util.Collection;

import org.slf4j.Logger;

public class ValidationUtils {

  private ValidationUtils() {
    // Empty private constructor to avoid instantiation.
  }

  /**
   * Handles a {@link ValidationResult} uniformly. Warnings are logged, errors raise exceptions.
   *
   * @param validationResult The {@link ValidationResult} obtained using an {@link ArtifactAstValidator}.
   * @param logger           The {@link Logger} to use for logging.
   * @throws ConfigurationException If there was any result item with {@link ERROR} level.
   */
  public static void handleValidationResult(ValidationResult validationResult, Logger logger)
      throws ConfigurationException {
    final Collection<ValidationResultItem> items = validationResult.getItems();

    // Logs warnings if any.
    items.stream()
        .filter(v -> v.getValidation().getLevel().equals(WARN))
        .forEach(v -> logger.warn(validationResultItemToString(v)));

    // If there are any errors, throws.
    final boolean hasErrors = items.stream().anyMatch(v -> v.getValidation().getLevel().equals(ERROR));
    if (hasErrors) {
      throw new ConfigurationException(createStaticMessage(validationResult.getItems()
          .stream()
          .map(ValidationUtils::validationResultItemToString)
          .collect(joining(lineSeparator()))));
    }
  }

  private static String validationResultItemToString(ValidationResultItem v) {
    return v.getComponents().stream()
        .map(component -> component.getMetadata().getFileName().orElse("unknown") + ":"
            + component.getMetadata().getStartLine().orElse(-1))
        .collect(joining("; ", "[", "]")) + ": " + v.getMessage();
  }
}
