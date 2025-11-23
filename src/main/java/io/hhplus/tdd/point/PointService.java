package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    /**
     * 특정 유저의 현재 포인트 조회
     */
    public UserPoint getPoint(long userId) {
        return userPointTable.selectById(userId);
    }

    /**
     * 특정 유저의 포인트 사용/충전 내역 조회
     */
    public List<PointHistory> getHistories(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * 포인트 충전
     */
    public UserPoint charge(long userId, long amount) {
        validateAmount(amount);

        UserPoint current = userPointTable.selectById(userId);
        long newPoint = safeAdd(current.point(), amount);

        UserPoint updated = userPointTable.insertOrUpdate(userId, newPoint);
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, updated.updateMillis());

        return updated;
    }

    /**
     * 포인트 사용
     */
    public UserPoint use(long userId, long amount) {
        validateAmount(amount);

        UserPoint current = userPointTable.selectById(userId);

        if (current.point() < amount) {
            // 잔고 부족 시 실패
            throw new IllegalStateException("포인트가 부족합니다.");
        }

        long newPoint = current.point() - amount;
        UserPoint updated = userPointTable.insertOrUpdate(userId, newPoint);
        pointHistoryTable.insert(userId, amount, TransactionType.USE, updated.updateMillis());

        return updated;
    }

    private void validateAmount(long amount) {
        // 0이하 금액에 대한 방어 로직
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private long safeAdd(long a, long b) {
        // long 오버플로우에 대한 방어
        return Math.addExact(a, b);
    }
}