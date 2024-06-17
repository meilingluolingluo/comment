package com.mll.utils;

import jakarta.annotation.Resource;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedissonLock {
    @Resource
    private RedissonClient redissonClient;

    public void executeWithRetryLock() {
        RLock lock = redissonClient.getLock("retryLock");

        try {
            // 尝试获取锁，等待时间为100秒，锁持有时间为10分钟，重试间隔为2秒
            if (lock.tryLock(100, 600, TimeUnit.SECONDS)) {
                try {
                    // 临界区代码
                    System.out.println("Acquired lock and executing critical section");
                    // 模拟业务处理
                    Thread.sleep(5000);
                } finally {
                    lock.unlock();
                    System.out.println("Released lock");
                }
            } else {
                System.out.println("Failed to acquire lock");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Interrupted while trying to acquire lock");
        }
    }
    public void executeWithReentrantLock() {
        RLock lock = redissonClient.getLock("reentrantLock");

        try {
            // 第一次获取锁
            if (lock.tryLock(10, 60, TimeUnit.SECONDS)) {
                try {
                    System.out.println("Acquired lock for the first time");

                    // 第二次获取同一把锁（重入）
                    if (lock.tryLock(10, 60, TimeUnit.SECONDS)) {
                        try {
                            System.out.println("Acquired lock for the second time");

                            // 模拟业务处理
                            Thread.sleep(5000);
                        } finally {
                            lock.unlock();
                            System.out.println("Released lock for the second time");
                        }
                    }
                } finally {
                    lock.unlock();
                    System.out.println("Released lock for the first time");
                }
            } else {
                System.out.println("Failed to acquire lock");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Interrupted while trying to acquire lock");
        }
    }
    public void executeWithWatchdog() {
        RLock lock = redissonClient.getLock("watchdogLock");

        try {
            // 尝试获取锁
            lock.lock(10, TimeUnit.SECONDS);
            try {
                System.out.println("Acquired lock and executing critical section");

                // 模拟业务处理时间超过锁的持有时间，观察Watchdog机制
                Thread.sleep(15000);
            } finally {
                lock.unlock();
                System.out.println("Released lock");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Interrupted while trying to acquire lock");
        }
    }

    public void executeWithMultiLock() {
        // 创建三个独立的锁
        RLock lock1 = redissonClient.getLock("lock1");
        RLock lock2 = redissonClient.getLock("lock2");
        RLock lock3 = redissonClient.getLock("lock3");

        // 创建多重锁
        RedissonMultiLock multiLock = new RedissonMultiLock(lock1, lock2, lock3);
        try {
            // 尝试获取多重锁
            if (multiLock.tryLock(10, 60, TimeUnit.SECONDS)) {
                try {
                    // 临界区代码
                    System.out.println("Acquired all locks and executing critical section");
                    // 模拟业务处理
                    Thread.sleep(5000);
                } finally {
                    multiLock.unlock();
                    System.out.println("Released all locks");
                }
            } else {
                System.out.println("Failed to acquire all locks");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Interrupted while trying to acquire locks");
        }
    }

}
