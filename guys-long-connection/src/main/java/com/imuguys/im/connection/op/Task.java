package com.imuguys.im.connection.op;

import io.reactivex.Observable;

/**
 * 任务
 * @param <T> 任务结果类型
 */
public interface Task<T> {
    /**
     * 获取任务
     */
    Observable<T> getTaskObservable();
}
