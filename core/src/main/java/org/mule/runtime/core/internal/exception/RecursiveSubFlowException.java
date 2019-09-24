package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.processor.Processor;

public class RecursiveSubFlowException extends LifecycleException {

  public RecursiveSubFlowException(String offendingFlowName, Processor flowRefProcessor) {
    super(createStaticMessage("Found a possible infinite recursion involving flow named " + offendingFlowName), flowRefProcessor);

  }
}
