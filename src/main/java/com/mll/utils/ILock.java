package com.mll.utils;

public interface ILock {

    boolean tryLock(Long timeoutSec);

    void unlock();
}
