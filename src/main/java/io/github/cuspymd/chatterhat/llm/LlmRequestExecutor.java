package io.github.cuspymd.chatterhat.llm;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LlmRequestExecutor {
	private final ExecutorService executorService = Executors.newFixedThreadPool(2);

	public <T> CompletableFuture<T> submit(CheckedSupplier<T> supplier) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return supplier.get();
			} catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		}, this.executorService);
	}

	public void shutdown() {
		this.executorService.shutdownNow();
	}

	@FunctionalInterface
	public interface CheckedSupplier<T> {
		T get() throws Exception;
	}
}
