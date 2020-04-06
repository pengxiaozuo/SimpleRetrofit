package com.peng.retrofit.sample;

import com.peng.retrofit.Call;
import com.peng.retrofit.Response;
import com.peng.retrofit.Retrofit;
import com.peng.retrofit.http.Get;
import com.peng.retrofit.http.Query;
import okhttp3.ResponseBody;

import java.io.IOException;

public class SampleTest {
    public static void main(String[] args) throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://localhost/")
            .build();
        ApiService apiService = retrofit.create(ApiService.class);
        Call<ResponseBody> call = apiService.test("hello");
        Response<ResponseBody> execute = call.execute();
        ResponseBody body = execute.body();
        String string = body.string();
        System.out.println(string);
    }

    interface ApiService {
        /*
        下面是php的简单代码
        <?php
        $ret['msg'] = '成功';
        $ret['data'] = [
            'test' => $_GET['test']??'-----',
        ];
        header("Content-type: application/json");
        die(json_encode($ret));
         */
        @Get("/test.php")
        Call<ResponseBody> test(
            @Query("test") String test
        );
    }
}
