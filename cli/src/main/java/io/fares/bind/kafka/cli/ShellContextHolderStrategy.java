package io.fares.bind.kafka.cli;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * The strategy to retrieve the current {@link ShellContext} when the shell is opened interactively.
 */
public interface ShellContextHolderStrategy {

  /**
   * Clears the current shell context.
   */
  void clearContext();

  /**
   * Get the current shell context.
   *
   * @return the current {@link ShellContext}
   */
  @NotNull ShellContext getContext();

  /**
   * Sets the current shell context.
   *
   * @param context the current {@link ShellContext}
   */
  void setContext(@NotNull ShellContext context);

  /**
   * Get a {@link Supplier} that returns the current shell context.
   *
   * @return the {@link Supplier} that returns the current {@link ShellContext}
   */
  default @NotNull Supplier<ShellContext> getDeferredContext() {
    return this::getContext;
  }

  /**
   * Set a {@link Supplier} that will return the current shell context.
   *
   * @param deferredContext a {@link Supplier} that returns the {@link ShellContext}
   */
  default void setDeferredContext(@NotNull Supplier<ShellContext> deferredContext) {
    setContext(deferredContext.get());
  }

}
