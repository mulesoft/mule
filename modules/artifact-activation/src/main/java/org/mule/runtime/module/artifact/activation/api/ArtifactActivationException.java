package org.mule.runtime.module.artifact.activation.api;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

@NoExtend
public class ArtifactActivationException extends MuleRuntimeException {

  public ArtifactActivationException(I18nMessage message) {
    super(message);
  }

  public ArtifactActivationException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
