package com.peng.retrofit;

import java.io.IOException;

/**
 * 网络请求接口
 * @param <T>
 */
public interface Call<T> {
    /**
     * 同步请求
     * @return
     * @throws IOException
     */
    Response<T> execute() throws IOException;
}
