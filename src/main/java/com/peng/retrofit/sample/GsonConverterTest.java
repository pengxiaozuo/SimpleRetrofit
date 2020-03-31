package com.peng.retrofit.sample;

import com.peng.retrofit.Call;
import com.peng.retrofit.Response;
import com.peng.retrofit.Retrofit;
import com.peng.retrofit.converter.GsonConverterFactory;
import com.peng.retrofit.http.Get;
import com.peng.retrofit.http.Query;
import com.peng.retrofit.sample.entity.User;

import java.io.IOException;

public class GsonConverterTest {
    public static void main(String[] args) throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://localhost")
            .addConverterFactory(GsonConverterFactory.create())
            .build();
        ApiService apiService = retrofit.create(ApiService.class);
        Call<User> call = apiService.test("Tom", "" + 20);
        Response<User> execute = call.execute();
        User body = execute.body();
        String string = body.toString();
        System.out.println(string);
    }

    interface ApiService {
        /*
        下面是php的简单代码
        <?php
        $name = $_GET['name']??'请传入name查询参数';
        $age = $_GET['age']??0;
        $ret = [
            'name' => $name,
            'age' => $age,
        ];
        header("Content-type: application/json");
        die(json_encode($ret));
         */
        @Get("/userTest.php")
        Call<User> test(
            @Query("name") String name,
            @Query("age") String age
        );
    }
}
