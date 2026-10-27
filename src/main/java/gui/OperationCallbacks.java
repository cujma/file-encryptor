package gui;

import java.util.Objects;
import java.util.function.Consumer;

public record OperationCallbacks(
        Consumer<String> onStatus,
        Consumer<String> onError,
        Consumer<Double> onProgress,
        Runnable onFinished
) {
    public OperationCallbacks {
        Objects.requireNonNull(onStatus, "Status callback cannot be null");
        Objects.requireNonNull(onError, "Error callback cannot be null");
        Objects.requireNonNull(onProgress, "Progress callback cannot be null");
        Objects.requireNonNull(onFinished, "Finished callback cannot be null");
    }
}
