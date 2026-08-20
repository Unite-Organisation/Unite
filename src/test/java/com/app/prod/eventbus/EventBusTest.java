package com.app.prod.eventbus;

import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;

class EventBusTest {

    private final List<AppEvent> handled = new ArrayList<>();
    private final RecordingHandler handler = new RecordingHandler(handled);
    private final EventDispatcher dispatcher = new EventDispatcher(Runnable::run);

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void dispatchesEventToItsHandler() {
        EventBus eventBus = new EventBus(List.of(handler), dispatcher);
        TestEvent event = new TestEvent(UUID.randomUUID());

        eventBus.publish(event);

        assertThat(handled).containsExactly(event);
    }

    @Test
    void refusesToStartWhenTwoHandlersClaimTheSameEvent() {
        List<EventHandler<? extends AppEvent>> handlers = List.of(handler, new RecordingHandler(handled));

        assertThatThrownBy(() -> new EventBus(handlers, dispatcher))
                .isInstanceOf(IllegalApplicationStateException.class)
                .asInstanceOf(throwable(IllegalApplicationStateException.class))
                .extracting(exception -> exception.getAppErrors().getFirst())
                .satisfies(error -> {
                    assertThat(error.code()).isEqualTo(Code.EVENT_HANDLER_DUPLICATED);
                    assertThat(error.message()).contains("TestEvent");
                });
    }

    @Test
    void failsLoudlyWhenNoHandlerIsRegistered() {
        EventBus eventBus = new EventBus(List.of(), dispatcher);

        assertThatThrownBy(() -> eventBus.publish(new TestEvent(UUID.randomUUID())))
                .isInstanceOf(IllegalApplicationStateException.class);
    }

    @Test
    void holdsEventPublishedInsideTransactionUntilCommit() {
        EventBus eventBus = new EventBus(List.of(handler), dispatcher);
        TestEvent event = new TestEvent(UUID.randomUUID());
        TransactionSynchronizationManager.initSynchronization();

        eventBus.publish(event);

        assertThat(handled).isEmpty();

        commit();

        assertThat(handled).containsExactly(event);
    }

    @Test
    void neverDispatchesWhenTransactionRollsBack() {
        EventBus eventBus = new EventBus(List.of(handler), dispatcher);
        TransactionSynchronizationManager.initSynchronization();

        eventBus.publish(new TestEvent(UUID.randomUUID()));
        rollback();

        assertThat(handled).isEmpty();
    }

    private static void commit() {
        List.copyOf(TransactionSynchronizationManager.getSynchronizations())
                .forEach(TransactionSynchronization::afterCommit);
    }

    private static void rollback() {
        List.copyOf(TransactionSynchronizationManager.getSynchronizations())
                .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    }

    private record TestEvent(UUID id) implements AppEvent {
    }

    private record RecordingHandler(List<AppEvent> handled) implements EventHandler<TestEvent> {

        @Override
        public void handle(TestEvent event) {
            handled.add(event);
        }

        @Override
        public Class<TestEvent> eventType() {
            return TestEvent.class;
        }
    }
}
