package com.app.prod.eventbus;

/**
 * A fact that already happened, handed to {@link EventBus} so the request thread does not wait for
 * whatever reacts to it. Unlike {@link com.app.prod.job.jobs.Job} an event is never persisted and
 * never retried - it runs exactly once, on a best effort basis.
 * <p>
 * Implementations are records carrying identifiers and plain values only, never jOOQ records and
 * never a {@link com.app.prod.access.BuildingScope} - the handler runs long after the request that
 * produced the event was authorized.
 */
public interface AppEvent {
}
