package com.peng.retrofit;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * 请求参数等构建
 */
class RequestBuilder {
    private final String method;
    private final String baseUrl;
    private String relativeUrl;
    private HttpUrl.Builder urlBuilder;
    private RequestBody body;
    private final Request.Builder requestBuilder;
    private MultipartBody.Builder multipartBuilder;
    private FormBody.Builder formBuilder;

    RequestBuilder(String method, String baseUrl, String relativeUrl, boolean isFormEncoded, boolean isMultipart) {
        this.method = method;
        this.baseUrl = baseUrl;
        this.relativeUrl = relativeUrl;
        this.requestBuilder = new Request.Builder();
        if (isFormEncoded) {
            formBuilder = new FormBody.Builder();
        }
        if (isMultipart) {
            multipartBuilder = new MultipartBody.Builder();
            multipartBuilder.setType(MultipartBody.FORM);
        }
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

    void addFormField(String name, boolean encoded, String value) {
        if (encoded) {
            formBuilder.addEncoded(name, value);
        } else {
            formBuilder.add(name, value);
        }
    }

    void addPart(MultipartBody.Part part) {
        multipartBuilder.addPart(part);
    }


    void addPart(Headers headers, RequestBody body) {
        multipartBuilder.addPart(headers, body);
    }

    void setBody(RequestBody body) {
        this.body = body;
    }

    Request.Builder get() {
        HttpUrl url;
        HttpUrl.Builder urlBuilder = this.urlBuilder;
        if (urlBuilder != null) {
            url = urlBuilder.build();
        } else {
            url = HttpUrl.parse(baseUrl).resolve(relativeUrl);
        }
        RequestBody body = this.body;
        if (body == null) {
            if (formBuilder != null) {
                body = formBuilder.build();
            }

            if (multipartBuilder != null) {
                body = multipartBuilder.build();
            }
        }
        return requestBuilder
            .url(url)
            .method(method, body);
    }
}
