package eu.europeana.postpublication.batch;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe counter to track new, updated and failed Records when running post publication pipeline.
 *
 * <p>Before use, call {@link BatchSyncStats#reset()}
 */
@Component
public class BatchSyncStats {

    private final AtomicInteger updatedRecords = new AtomicInteger();
    private final AtomicInteger failedRecords = new AtomicInteger();

    private Instant startTime;
    private Duration elapsedTime;

    public void reset() {
        updatedRecords.set(0);
        failedRecords.set(0);

        startTime = null;
        elapsedTime = Duration.ZERO;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public void setElapsedTime(Duration elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public void addFailed() {
        failedRecords.incrementAndGet();
    }

    public void addUpdated() {
        updatedRecords.incrementAndGet();
    }

    public int getUpdated() {
        return updatedRecords.get();
    }

    public Duration getElapsedTime() {
        return elapsedTime;
    }

    public Instant getStartTime() {
        return startTime;
    }

}