package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * PointService 단위테스트
 */
class PointServiceTest {

    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @Test
    @DisplayName("새로 조회한 유저는 포인트가 0이어야 한다")
    // 이유: UserPointTable.selectById는 없는 유저에 대해 empty(id)를 반환해야 한다.
    void getPoint_newUser_hasZeroPoint() {
        long userId = 1L;
        UserPoint result = pointService.getPoint(userId);

        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isZero();
    }

    @Test
    @DisplayName("포인트를 충전하면 현재 포인트가 증가하고, 히스토리가 기록된다")
    // 이유: 충전 기능의 기본 시나리오
    void charge_increasePoint_andWriteHistory() {
        long userId = 1L;
        UserPoint charged = pointService.charge(userId, 1_000L);

        assertThat(charged.point()).isEqualTo(1_000L);

        List<PointHistory> histories = pointService.getHistories(userId);
        assertThat(histories).hasSize(1);
        PointHistory history = histories.get(0);
        assertThat(history.userId()).isEqualTo(userId);
        assertThat(history.amount()).isEqualTo(1_000L);
        assertThat(history.type()).isEqualTo(TransactionType.CHARGE);
    }

    @Test
    @DisplayName("여러 번 충전하면 누적된 금액이 잔고로 반영된다")
    // 이유: 누적 충전 시에도 합산이 정확한지 확인한다.
    void charge_multipleTimes_accumulatesPoint() {
        long userId = 1L;
        pointService.charge(userId, 1_000L);
        UserPoint result =  pointService.charge(userId, 2_000L);

        assertThat(result.point()).isEqualTo(3_000L);

        List<PointHistory> histories = pointService.getHistories(userId);
        assertThat(histories).hasSize(2);
    }

    @Test
    @DisplayName("포인트를 사용하면 잔고가 감소하고, USE 히스토리가 기록된다")
    // 이유: 사용 기능의 정상 시나리오
    void use_decreasePoint_andWriteHistory() {
        long userId = 1L;
        pointService.charge(userId, 2_000L);

        UserPoint used = pointService.use(userId, 500L);
        assertThat(used.point()).isEqualTo(1_500L);

        List<PointHistory> histories = pointService.getHistories(userId);
        assertThat(histories).hasSize(2);
        PointHistory last = histories.get(1);
        assertThat(last.type()).isEqualTo(TransactionType.USE);
        assertThat(last.amount()).isEqualTo(500L);
    }

    @Test
    @DisplayName("잔고보다 많은 금액을 사용하려고 하면 예외가 발생해야 한다")
    // 이유: 잔고가 부족할 경우, 포인트 사용 실패
    void use_moreThanBalance_shouldFail() {
        long userId = 1L;
        pointService.charge(userId, 1_000L);
        assertThatThrownBy(() -> pointService.use(userId, 2_000L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("포인트가 부족");
    }

    @Test
    @DisplayName("0원 이하 금액으로 충전하면 에외 발생해야 한다")
    // 이유: 충전 기능에서 잘못된 입력에 대한 방어 로직 검증
    void charge_zeroOrNegativeAmount_shouldFail() {
        long userId = 1L;

        assertThatThrownBy(() -> pointService.charge(userId, 0L))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> pointService.charge(userId, -100L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("0원 이하 금액으로 사용 요청 시 예외 발생해야 한다 ")
    // 이유: 사용 기능에서 잘못된 입력에 대한 방어 로직 검증
    void use_zeroOrNegativeAmount_shouldFail() {
        long userId = 1L;

        assertThatThrownBy(() -> pointService.use(userId, 0L))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> pointService.use(userId, -100L))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
