package com.peng.retrofit.sample;

import com.peng.retrofit.Call;
import com.peng.retrofit.Response;
import com.peng.retrofit.Retrofit;
import com.peng.retrofit.calladapter.RxJavaCallAdapterFactory;
import com.peng.retrofit.converter.GsonConverterFactory;
import com.peng.retrofit.http.Get;
import com.peng.retrofit.http.Query;
import com.peng.retrofit.sample.entity.User;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;

import java.io.IOException;

public class RxJavaSampleTest {
    public static void main(String[] args) throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://localhost/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .build();
        ApiService apiService = retrofit.create(ApiService.class);
        apiService.test("Tom", "" + 20)
            .subscribe(new Observer<User>() {
                @Override
                public void onSubscribe(@NonNull Disposable d) {

                }

                @Override
                public void onNext(@NonNull User user) {
                    System.out.println(user.toString());
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    e.printStackTrace();
                }

                @Override
                public void onComplete() {
                    System.out.println("完成");
                }
            });

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
        Observable<User> test(
            @Query("name") String name,
            @Query("age") String age
        );
    }
}
