package com.peng.retrofit;


public class Response<T> {
    private final okhttp3.Response rawResponse;
    private final T body;

    private Response(okhttp3.Response rawResponse, T body) {
        this.rawResponse = rawResponse;
        this.body = body;
    }

    public static <T> Response<T> success(T body, okhttp3.Response response) {

        return new Response<>(response, body);
    }

    public okhttp3.Response raw() {
        return rawResponse;
    }

    public T body() {
        return body;
    }
}
