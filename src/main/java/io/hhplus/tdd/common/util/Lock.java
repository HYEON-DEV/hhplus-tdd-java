package io.hhplus.tdd.common.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class Lock {
    private final ConcurrentMap<Long, ReentrantLock> lockTable = new ConcurrentHashMap<>();

    private ReentrantLock getLock(long userId) {
        return lockTable.computeIfAbsent(userId, id -> new ReentrantLock(true));
    }

    public void lock(long userId) {
        getLock(userId).lock();
    }

    public void unlock(long userId) {
        ReentrantLock lock = lockTable.get(userId);
        // 잠겨있지 않은데 unlock을 호출하면 오류
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 락 + 실행을 한 번에 감사는 execute
     */
    public <T> T execute(long userId, Supplier<T> task) {
        ReentrantLock lock = getLock(userId);
        lock.lock();
        try {
            return task.get();
        } finally {
            lock.unlock();
        }
    }

    public void run(long userId, Runnable task) {
        ReentrantLock lock = getLock(userId);
        lock.lock();
        try {
            task.run();
        } finally {
            lock.unlock();
        }
    }
}
