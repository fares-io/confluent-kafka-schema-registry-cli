package io.fares.bind.kafka.cli;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ShellContextHolder {

  private static ShellContextHolderStrategy strategy;

  private static int initializeCount = 0;

  static {
    initialize();
  }

  private static void initialize() {
    initializeStrategy();
    initializeCount++;
  }

  private static void initializeStrategy() {
    // the only strategy implementation we have so far
    strategy = new ThreadLocalShellContextHolderStrategy();
  }

  /**
   * see {@link ShellContextHolderStrategy#clearContext()}
   */
  public static void clearContext() {
    strategy.clearContext();
  }

  /**
   * see {@link ShellContextHolderStrategy#getContext()}
   */
  public static ShellContext getContext() {
    return strategy.getContext();
  }

  /**
   * see {@link ShellContextHolderStrategy#setContext(ShellContext)}
   */
  public static void setContext(ShellContext context) {
    strategy.setContext(context);
  }

  /**
   * see {@link ShellContextHolderStrategy#getDeferredContext()}
   */
  public static Supplier<ShellContext> getDeferredContext() {
    return strategy.getDeferredContext();
  }

  /**
   * see {@link ShellContextHolderStrategy#setDeferredContext(Supplier)}
   */
  public static void setDeferredContext(Supplier<ShellContext> deferredContext) {
    strategy.setDeferredContext(deferredContext);
  }

  /**
   * Get the context strategy.
   *
   * @return the configured strategy for storing the shell context
   */
  public static ShellContextHolderStrategy getContextHolderStrategy() {
    return strategy;
  }

  /**
   * Set the strategy used to store the shell context.
   *
   * @param strategy the shell context
   */
  public static void setContextHolderStrategy(@NotNull ShellContextHolderStrategy strategy) {
    ShellContextHolder.strategy = strategy;
    initialize();
  }

  @Override
  public String toString() {
    return "SecurityContextHolder[strategy='" + strategy.getClass().getSimpleName() + "'; initializeCount="
      + initializeCount + "]";
  }

}
