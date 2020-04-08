package com.peng.retrofit.sample;

import com.peng.retrofit.Call;
import com.peng.retrofit.Response;
import com.peng.retrofit.Retrofit;
import com.peng.retrofit.http.Body;
import com.peng.retrofit.http.Field;
import com.peng.retrofit.http.FormUrlEncoded;
import com.peng.retrofit.http.Multipart;
import com.peng.retrofit.http.Part;
import com.peng.retrofit.http.Post;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class PostTest {

    public static void main(String[] args) throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://localhost/")
            .build();
        ApiService apiService = retrofit.create(ApiService.class);
        Call<ResponseBody> call = apiService.postFrom("Tom");
        Response<ResponseBody> response = call.execute();
        ResponseBody body = response.body();
        String string = body.string();
        System.out.println(string);

        System.out.println(apiService.postFrom("Jom").execute().body().string());
        System.out.println(apiService.post(RequestBody.create(MediaType.parse("text/plan"),"hello")).execute().body().string());
    }

    interface ApiService {
        /*
        postTest.php
        <?php
        $mthod = $_GET['method'];
        $mthod();

        function post()
        {
            $params = file_get_contents("php://input");
            returnJson(['rec'=>$params]);
        }

        function postForm()
        {
            $name = $_POST['name'];
            returnJson(['recName'=>$name]);
        }

        function postMultipart()
        {
            $name = $_POST['name'];
            returnJson(['recName'=>$name]);
        }

        function returnJson($data) {
            header("Content-type: application/json");
            die(json_encode($data));
        }
         */
        @Post("/postTest.php?method=postForm")
        @FormUrlEncoded
        Call<ResponseBody> postFrom(@Field("name") String name);

        @Post("/postTest.php?method=postMultipart")
        @Multipart
        Call<ResponseBody> postMultipart(@Part MultipartBody.Part part);

        @Post("/postTest.php?method=post")
        Call<ResponseBody> post(@Body RequestBody body);
    }
}
