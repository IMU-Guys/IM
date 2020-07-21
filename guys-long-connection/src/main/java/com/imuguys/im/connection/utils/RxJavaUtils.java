package com.imuguys.im.connection.utils;

import androidx.annotation.Nullable;

import io.reactivex.disposables.Disposable;

public class RxJavaUtils {
    public static void dispose(@Nullable Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
