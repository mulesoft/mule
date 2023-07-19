/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

public class OverrideMe implements Processor {

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return CoreEvent.builder(event).message(Message.builder(event.getMessage()).value(getPayload()).build()).build();
  }

  private String getPayload() {
    return "This is an embedded library class";
  }

}
