package com.peng.retrofit;

import okhttp3.HttpUrl;
import okhttp3.Request;

/**
 * 请求参数等构建
 */
class RequestBuilder {
    private final String method;
    private final String baseUrl;
    private String relativeUrl;
    private HttpUrl.Builder urlBuilder;

    private final Request.Builder requestBuilder;

    RequestBuilder(String method, String baseUrl, String relativeUrl) {
        this.method = method;
        this.baseUrl = baseUrl;
        this.relativeUrl = relativeUrl;
        this.requestBuilder = new Request.Builder();
    }

    /**
     * 添加query参数 (?name=value&name1=value1)
     */
    void addQueryParam(String name, String value, boolean encoded) {
        if (relativeUrl != null) {
            urlBuilder = HttpUrl.parse(baseUrl).newBuilder(relativeUrl);
            relativeUrl = null;
        }
        if (encoded) {
            urlBuilder.addEncodedQueryParameter(name, value);
        } else {
            urlBuilder.addQueryParameter(name, value);
        }
    }

    /**
     * 替换资源路径
     */
    void addPathParam(String name, String value) {
        relativeUrl = relativeUrl.replace("{" + name + "}",value);
    }

    Request.Builder get() {
        HttpUrl url;
        HttpUrl.Builder urlBuilder = this.urlBuilder;
        if (urlBuilder != null) {
            url = urlBuilder.build();
        } else {
            url = HttpUrl.parse(baseUrl).resolve(relativeUrl);
        }
        return requestBuilder
            .url(url)
            .method(method, null);
    }
}
