package io.hhplus.tdd.point;

import io.hhplus.tdd.common.util.Lock;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class PointServiceConcurrencyTest {
    UserPointTable userPointTable;
    PointHistoryTable pointHistoryTable;
    PointService pointService;
    Lock lock;

    long userId = 1L;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        lock = new Lock();
        pointService = new PointService(userPointTable, pointHistoryTable, lock);
        // 초기 잔액 = 0
        userPointTable.insertOrUpdate(userId, 0L);
    }

    @Test
    @DisplayName("동시에 100번 충전해도 최종 잔액을 정확하게 누적된다")
    void concurrent_charge() throws Exception {
        int threadCount = 100;
        long amount = 100L;

        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i=0; i<threadCount; i++) {
            executor.submit(() -> {
                try{
                    pointService.charge(userId, amount);
                }finally{
                    latch.countDown();
                }
            });
        }

        latch.await();

        UserPoint userPoint = pointService.getPoint(userId);
        List<PointHistory> histories = pointService.getHistories(userId);

        assertThat(userPoint.point()).isEqualTo(amount * threadCount);

        long chargeHistoryCount = histories.stream()
            .filter(h -> h.type() == TransactionType.CHARGE)
            .count();
        assertThat(chargeHistoryCount).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("동시에 100번 사용해도 Lock 덕분에 정확히 반영된다")
    void concurrent_use() throws Exception {
        pointService.charge(userId, 10_000L);

        int threadCount = 100;
        long useAmount = 100L;

        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i=0; i<threadCount; i++) {
            executor.submit(() -> {
                try{
                    pointService.use(userId, useAmount);
                }finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        UserPoint after = pointService.getPoint(userId);

        assertThat(after.point()).isEqualTo(10_000L - (useAmount * threadCount));

        long useHistoryCount = pointService.getHistories(userId).stream()
            .filter(h -> h.type() == TransactionType.USE)
            .count();

        assertThat(useHistoryCount).isEqualTo(threadCount);
    }
}
