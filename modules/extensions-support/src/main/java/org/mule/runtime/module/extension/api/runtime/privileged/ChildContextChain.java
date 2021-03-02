package org.mule.runtime.module.extension.api.runtime.privileged;

import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.route.Chain;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * bla bla
 * @since 4.4.0
 */
public interface ChildContextChain extends Chain {

  void process(String correlationId, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError);

}
