package com.peng.retrofit.calladapter;

import com.peng.retrofit.Call;
import com.peng.retrofit.CallAdapter;
import com.peng.retrofit.Response;
import com.peng.retrofit.Retrofit;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class RxJavaCallAdapterFactory implements CallAdapter.Factory {

    public static RxJavaCallAdapterFactory create() {
        return new RxJavaCallAdapterFactory();
    }

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] animations, Retrofit retrofit) {
        if (returnType instanceof ParameterizedType) {
            ParameterizedType t = (ParameterizedType) returnType;
            if (t.getRawType() == Observable.class) {
                return new RxJavaCallAdapter<>(t.getActualTypeArguments()[0]);
            }
        }
        return null;
    }

    static final class RxJavaCallAdapter<T> implements CallAdapter<T, Object> {
        Type returnType;

        public RxJavaCallAdapter(Type returnType) {
            this.returnType = returnType;
        }

        @Override
        public Type responseType() {
            return returnType;
        }

        @Override
        public Object adapt(Call<T> call) {
            Observable<T> observable = new CallExecuteObservable<>(call);

            return RxJavaPlugins.onAssembly(observable);
        }
    }

    static final class CallExecuteObservable<T> extends Observable<T> {
        private Call<T> call;

        public CallExecuteObservable(Call<T> call) {
            this.call = call;
        }

        @Override
        protected void subscribeActual(Observer<? super T> observer) {
            try {
                Response<T> response = call.execute();
                observer.onNext(response.body());
                observer.onComplete();
            } catch (IOException e) {
                observer.onError(e);
            }
        }
    }
}
