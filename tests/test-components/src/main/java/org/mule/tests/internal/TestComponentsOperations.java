package org.mule.tests.internal;

import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class TestComponentsOperations {

  /**
   * A stores received events in a in-memory queue. Events can be consumed using mule client requests.
   * 
   * @param content Content to be sent to the queue. By default it will be the payload content.
   */
  @MediaType(value = ANY, strict = false)
  public void queuePush(@Config QueueConfiguration configuration,
                        @Connection TestQueue queue,
                        @Content(primary = true) @Optional(defaultValue = "#[payload]") TypedValue<Object> content,
                        @Optional(defaultValue = "#[message]") TypedValue<Object> msg) throws InterruptedException {
    Message message = Message.builder((Message) msg.getValue()).payload(content).build();
    EventContext eventContext = create("testEvent", "dummy", fromSingleComponent("test"), NullExceptionHandler.getInstance());
    CoreEvent event = CoreEvent.builder(eventContext).message(message).build();
    queue.push(event);
  }
}
