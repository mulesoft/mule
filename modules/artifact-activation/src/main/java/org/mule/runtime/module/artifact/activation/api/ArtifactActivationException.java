package org.mule.runtime.module.artifact.activation.api;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Indicates that an error occurred while operating on an artifact.
 *
 * @since 4.5
 */
@NoExtend
public class ArtifactActivationException extends MuleRuntimeException {

  /**
   * @param message the exception message
   */
  public ArtifactActivationException(I18nMessage message) {
    super(message);
  }
}
