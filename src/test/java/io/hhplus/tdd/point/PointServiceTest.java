package io.hhplus.tdd.point;

import io.hhplus.tdd.common.exception.BaseException;
import io.hhplus.tdd.common.response.ErrorCode;
import io.hhplus.tdd.common.util.Lock;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


/**
 * PointService 단위테스트
 */
class PointServiceTest {

    UserPointTable userPointTable;
    PointHistoryTable pointHistoryTable;
    PointService pointService;
    Lock lock;

    @BeforeEach
    void setUp() {
        userPointTable = mock(UserPointTable.class);
        pointHistoryTable = mock(PointHistoryTable.class);
        lock = mock(Lock.class);
        pointService = new PointService(userPointTable, pointHistoryTable, lock);
    }

    @Nested
    @DisplayName("charge 메서드")
    class ChargeTest{
        long userId = 1L;
        long amount = 1_000L;
        UserPoint before = new UserPoint(userId, 2_000L, System.currentTimeMillis());
        UserPoint after = new UserPoint(userId, 3_000L, System.currentTimeMillis());

        @Test
        @DisplayName("정상 충전 시 Lock.execute 내부에서 select -> insert -> history 순서로 호출된다")
        void charge_success() {
            when(lock.execute(eq(userId), any()))
                .thenAnswer(invocation -> {
                return ((Supplier<UserPoint>) invocation.getArgument(1)).get();
            });
            when(userPointTable.selectById(userId))
                .thenReturn(before);
            when(userPointTable.insertOrUpdate(userId, before.point()+amount))
                .thenReturn(after);

            UserPoint result = pointService.charge(userId, amount);

            assertThat(result.point()).isEqualTo(3_000L);

            verify(lock, times(1))
                .execute(eq(userId), any());
            verify(userPointTable, times(1))
                .selectById(userId);
            verify(userPointTable, times(1))
                .insertOrUpdate(userId, 3_000L);
            verify(pointHistoryTable, times(1))
                .insert(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong());
        }

        @Test
        @DisplayName("충전 금액이 100원 미만이면 예외 발생")
        void charge_under_100() {
            assertThatThrownBy(() -> pointService.charge(userId, 50L))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("100원");
        }

        @Test
        @DisplayName("최대 잔액을 초과하면 예외 발생")
        void charge_over_max() {
            when(lock.execute(eq(userId), any()))
                .thenAnswer(invocation -> {
                    return ((Supplier<UserPoint>) invocation.getArgument(1)).get();
                });
            when(userPointTable.selectById(userId))
                .thenReturn(new UserPoint(userId, 99_500L, System.currentTimeMillis()));

            assertThatThrownBy(() -> pointService.charge(userId, 1_000L))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POINT_BALANCE_OVER);
        }
    }

    @Nested
    @DisplayName("use 메서드")
    class UseTest{
        long userId = 1L;

        @Test
        @DisplayName("정상 사용 시 잔액 차감 + history 기록")
        void use_success() {
            when(lock.execute(eq(userId), any())).thenAnswer(invocation -> {
                return ((Supplier<UserPoint>) invocation.getArgument(1)).get();
            });
            when(userPointTable.selectById(userId))
                .thenReturn(new UserPoint(userId, 5_000L, System.currentTimeMillis()));
            when(userPointTable.insertOrUpdate(userId, 4_000L))
                .thenReturn(new UserPoint(userId, 4_000L, System.currentTimeMillis()));

            UserPoint result = pointService.use(userId, 1_000L);

            assertThat(result.point()).isEqualTo(4_000L);

            verify(pointHistoryTable, times(1)).insert(
                eq(userId), eq(1_000L), eq(TransactionType.USE), anyLong());
        }

        @Test
        @DisplayName("잔액보다 큰 금액 사용 시 예외 발생")
        void use_over_balance() {
            when(lock.execute(eq(userId), any())).thenAnswer(invocation -> {
                return ((Supplier<UserPoint>) invocation.getArgument(1)).get();
            });
            when(userPointTable.selectById(userId)).thenReturn(
                new UserPoint(userId, 500L, System.currentTimeMillis()));

            assertThatThrownBy(() -> pointService.use(userId, 1_000L))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POINT_BALANCE_NEGATIVE);
        }
    }



//    @Test
//    @DisplayName("새로 조회한 유저는 포인트가 0이어야 한다")
//    // 이유: UserPointTable.selectById는 없는 유저에 대해 empty(id)를 반환해야 한다.
//    void getPoint_newUser_hasZeroPoint() {
//        long userId = 1L;
//        UserPoint result = pointService.getPoint(userId);
//
//        assertThat(result.id()).isEqualTo(userId);
//        assertThat(result.point()).isZero();
//    }

//    @Test
//    @DisplayName("여러 번 충전하면 누적된 금액이 잔고로 반영된다")
//    // 이유: 누적 충전 시에도 합산이 정확한지 확인한다.
//    void charge_multipleTimes_accumulatesPoint() {
//        long userId = 1L;
//        pointService.charge(userId, 1_000L);
//        UserPoint result =  pointService.charge(userId, 2_000L);
//
//        assertThat(result.point()).isEqualTo(3_000L);
//
//        List<PointHistory> histories = pointService.getHistories(userId);
//        assertThat(histories).hasSize(2);
//    }

}
