package com.peng.retrofit.sample;

import com.peng.retrofit.Call;
import com.peng.retrofit.Response;
import com.peng.retrofit.Retrofit;
import com.peng.retrofit.http.Get;
import com.peng.retrofit.http.Path;
import okhttp3.ResponseBody;

import java.io.IOException;

public class PathTest {
    public static void main(String[] args) throws IOException {

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://api.github.com")
            .build();
        GitHubApi github = retrofit.create(GitHubApi.class);
        Response<ResponseBody> response = github.contributors("square", "retrofit")
            .execute();
        ResponseBody body = response.body();
        String string = body.string();
        System.out.println(string);
    }

    interface GitHubApi {
        @Get("/repos/{owner}/{repo}/contributors")
        Call<ResponseBody> contributors(
            @Path("owner") String owner,
            @Path("repo") String repo
        );
    }
}
