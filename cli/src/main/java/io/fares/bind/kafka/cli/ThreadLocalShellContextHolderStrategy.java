package io.fares.bind.kafka.cli;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.util.function.Supplier;

public class ThreadLocalShellContextHolderStrategy implements ShellContextHolderStrategy {

  private static final ThreadLocal<Supplier<ShellContext>> contextHolder = new ThreadLocal<>();

  @Override
  public void clearContext() {
    contextHolder.remove();
  }

  @Override
  public @NotNull ShellContext getContext() {
    return getDeferredContext().get();
  }

  @Override
  public void setContext(@NotNull ShellContext context) {
    contextHolder.set(() -> context);
  }

  @Override
  public @NotNull Supplier<ShellContext> getDeferredContext() {
    Supplier<ShellContext> result = contextHolder.get();
    if (result == null) {
      ShellContext context = new ShellContext();
      result = () -> context;
      contextHolder.set(result);
    }
    return result;
  }

  @Override
  public void setDeferredContext(@NotNull Supplier<ShellContext> deferredContext) {
    Supplier<ShellContext> notNullDeferredContext = () -> {
      ShellContext result = deferredContext.get();
      Assert.notNull(result, "A Supplier<ShellContext> returned null and is not allowed.");
      return result;
    };
    contextHolder.set(notNullDeferredContext);
  }


}
