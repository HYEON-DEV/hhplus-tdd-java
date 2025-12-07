package io.hhplus.tdd.point;

import io.hhplus.tdd.common.response.ErrorCode;
import io.hhplus.tdd.common.exception.BaseException;
import io.hhplus.tdd.common.util.Lock;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {
    private static final long MIN_AMOUNT = 100L;
    private static final long MAX_BALANCE = 100_000L;

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final Lock lock;

    /**
     * 특정 유저의 현재 포인트 조회
     */
    public UserPoint getPoint(long userId) {
        validateUserId(userId);
        return userPointTable.selectById(userId);
    }

    /**
     * 특정 유저의 포인트 사용/충전 내역 조회
     */
    public List<PointHistory> getHistories(long userId) {
        validateUserId(userId);
        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * 포인트 충전
     */
    public UserPoint charge(long userId, long amount) {
        validateUserId(userId);
        validateAmount(amount);

        return lock.execute(userId, () -> {
            UserPoint current = userPointTable.selectById(userId);
            long newBalance = current.point() + amount;

            if (newBalance > MAX_BALANCE) {
                throw new BaseException(ErrorCode.POINT_BALANCE_OVER);
            }

            UserPoint updated = userPointTable.insertOrUpdate(userId, newBalance);
            pointHistoryTable.insert(
                userId,
                amount,
                TransactionType.CHARGE,
                System.currentTimeMillis()
            );
            return updated;
        });
    }

    /**
     * 포인트 사용
     */
    public UserPoint use(long userId, long amount) {
        validateUserId(userId);
        validateAmount(amount);

        return lock.execute(userId, () -> {
            UserPoint current = userPointTable.selectById(userId);
            long newBalance = current.point() - amount;

            if (newBalance < 0) {
                throw new BaseException(ErrorCode.POINT_BALANCE_NEGATIVE);
            }

            UserPoint updated = userPointTable.insertOrUpdate(userId, newBalance);
            pointHistoryTable.insert(
                userId,
                amount,
                TransactionType.USE,
                System.currentTimeMillis()
            );
            return updated;
        });
    }

    private void validateUserId(long userId) {
        if (userId <= 0) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateAmount(long amount) {
        if (amount < MIN_AMOUNT) {
            throw new BaseException(ErrorCode.POINT_LESS_THAN_100);
        }
    }
}