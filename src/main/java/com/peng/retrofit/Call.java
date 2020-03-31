package com.peng.retrofit;

import java.io.IOException;

public interface Call<T> {
    Response<T> execute() throws IOException;
}
