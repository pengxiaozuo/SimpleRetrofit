package com.peng.retrofit;

import okhttp3.ResponseBody;

import java.io.IOException;

public class OkHttpCall<T>implements Call<T> {
    RequestFactory requestFactory;
    Converter<ResponseBody,T> responseConverter;
    Object[] args;
    okhttp3.Call.Factory callFactory;
    private okhttp3.Call rawCall;

    OkHttpCall(RequestFactory requestFactory, Converter<ResponseBody, T> responseConverter,
                      Object[] args, okhttp3.Call.Factory callFactory) {
        this.requestFactory = requestFactory;
        this.responseConverter = responseConverter;
        this.args = args;
        this.callFactory = callFactory;
    }

    @Override
    public Response<T> execute() throws IOException {
        return parseResponse(getRawCall().execute());
    }

    /**
     * okhttp3.Response -> Response<T>
     */
    private Response<T> parseResponse(okhttp3.Response response) throws IOException {
        ResponseBody body = response.body();
        //转换ResponseBody -> T
        T t = responseConverter.convert(body);
        return Response.success(t, response);
    }

    private okhttp3.Call getRawCall() throws IOException {
        okhttp3.Call call = rawCall;
        if (call != null) return call;
        return rawCall = createRawCall();
    }

    private okhttp3.Call createRawCall() throws IOException {
        return callFactory.newCall(requestFactory.create(args));
    }
}
