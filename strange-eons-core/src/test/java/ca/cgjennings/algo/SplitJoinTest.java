package ca.cgjennings.algo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SplitJoinTest {

    @AfterEach
    void restoreDefaults() {
        SplitJoin.setParallelExecutionEnabled(true);
        SplitJoin.setHighPriorityThreadHint(false);
    }

    private static SplitJoin create(boolean parallel) {
        return parallel ? SplitJoin.createInstance(3) : new SerialSplitJoin();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void runsEverySubproblem(boolean parallel) throws ExecutionException {
        SplitJoin sj = create(parallel);
        try {
            AtomicInteger count = new AtomicInteger();
            Runnable[] tasks = new Runnable[10];
            for (int i = 0; i < tasks.length; ++i) {
                tasks[i] = count::incrementAndGet;
            }
            sj.run(tasks);
            sj.run(List.of(tasks));
            sj.runUnchecked(tasks);
            sj.runUnchecked(List.of(tasks));
            assertThat(count.get()).isEqualTo(40);
            assertThat(sj.getIdealSplitCount()).isPositive();
        } finally {
            sj.dispose();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void evaluatesInOrder(boolean parallel) throws ExecutionException {
        SplitJoin sj = create(parallel);
        try {
            List<Callable<Integer>> work = new ArrayList<>();
            for (int i = 0; i < 20; ++i) {
                final int n = i;
                work.add(() -> n * n);
            }
            List<Integer> squares = sj.evaluate(work);
            assertThat(squares).hasSize(20).startsWith(0, 1, 4, 9).endsWith(361);
            assertThat(sj.evaluateUnchecked(work)).isEqualTo(squares);
        } finally {
            sj.dispose();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void reportsFailures(boolean parallel) {
        SplitJoin sj = create(parallel);
        try {
            Runnable[] tasks = {() -> {}, () -> {
                throw new IllegalStateException("boom");
            }};
            assertThatThrownBy(() -> sj.run(tasks)).isInstanceOf(ExecutionException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> sj.runUnchecked(tasks)).isInstanceOf(RuntimeException.class);
            List<Callable<Integer>> bad = List.of(() -> {
                throw new IllegalArgumentException("bad");
            });
            assertThatThrownBy(() -> sj.evaluate(bad)).isInstanceOf(ExecutionException.class);
            assertThatThrownBy(() -> sj.evaluateUnchecked(bad)).isInstanceOf(RuntimeException.class);
        } finally {
            sj.dispose();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void executesSingleTasks(boolean parallel) throws InterruptedException {
        SplitJoin sj = create(parallel);
        try {
            CountDownLatch done = new CountDownLatch(1);
            sj.execute(done::countDown);
            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            sj.dispose();
        }
    }

    @Test
    void sharedInstanceFollowsParallelSetting() {
        SplitJoin.setParallelExecutionEnabled(false);
        assertThat(SplitJoin.getInstance()).isInstanceOf(SerialSplitJoin.class);
        assertThat(SplitJoin.createInstance()).isInstanceOf(SerialSplitJoin.class);
        SplitJoin.setHighPriorityThreadHint(true);
        assertThat(SplitJoin.getHighPriorityThreadHint()).isTrue();
    }
}
