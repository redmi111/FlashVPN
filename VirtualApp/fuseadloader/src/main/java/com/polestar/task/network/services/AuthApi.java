package com.polestar.task.network.services;

import com.polestar.task.network.datamodels.User;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface AuthApi {

    @Headers("Accept: application/json")
    @POST("api/v1/user/registerAnonymous")
    @FormUrlEncoded
    Call<User> registerAnonymous(@Field("device_id") String deviceID,
                                 @Field("version_code") int versionCode,
                                 @Field("pkg_name") String pkgName,
                                 @Field("secret") String secret,
                                 @Field("country") String country,
                                 @Field("lang") String lang,
                                 @Field("locale") String locale
                                 );

}
