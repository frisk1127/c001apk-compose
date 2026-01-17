package com.example.c001apk.compose.logic.network

import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.model.CheckCountResponse
import com.example.c001apk.compose.logic.model.CheckResponse
import com.example.c001apk.compose.logic.model.CreateFeedResponse
import com.example.c001apk.compose.logic.model.FeedContentResponse
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.model.LikeResponse
import com.example.c001apk.compose.logic.model.LoadUrlResponse
import com.example.c001apk.compose.logic.model.OSSUploadPrepareResponse
import com.example.c001apk.compose.logic.model.TotalReplyResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Created by bggRGjQaUbCoE on 2024/6/3
 */
interface ApiService {

    @GET("/v6/main/indexV8")
    fun getHomeFeed(
        @Query("page") page: Int,
        @Query("firstLaunch") firstLaunch: Int,
        @Query("installTime") installTime: String,
        @Query("firstItem") firstItem: String?,
        @Query("lastItem") lastItem: String?,
        @Query("ids") ids: String = EMPTY_STRING,
    ): Call<HomeFeedResponse>

    @GET
    fun getFeedContent(
        @Url url: String,
    ): Call<FeedContentResponse>

    @GET("/v6/feed/replyList")
    fun getFeedContentReply(
        @Query("id") id: String,
        @Query("listType") listType: String,
        @Query("page") page: Int,
        @Query("firstItem") firstItem: String?,
        @Query("lastItem") lastItem: String?,
        @Query("discussMode") discussMode: Int,
        @Query("feedType") feedType: String,
        @Query("blockStatus") blockStatus: Int,
        @Query("fromFeedAuthor") fromFeedAuthor: Int
    ): Call<HomeFeedResponse>

    @GET("/v6/search")
    fun getSearch(
        @Query("type") type: String,
        @Query("feedType") feedType: String,
        @Query("sort") sort: String,
        @Query("searchValue") keyWord: String,
        @Query("pageType") pageType: String?,
        @Query("pageParam") pageParam: String?,
        @Query("page") page: Int,
        @Query("lastItem") lastItem: String?,
        @Query("showAnonymous") showAnonymous: Int = -1
    ): Call<HomeFeedResponse>

    @GET("/v6/feed/replyList?listType=&discussMode=0&feedType=feed_reply&blockStatus=0&fromFeedAuthor=0")
    fun getReply2Reply(
        @Query("id") id: String,
        @Query("page") page: Int,
        @Query("lastItem") lastItem: String?
    ): Call<HomeFeedResponse>

    @GET("/v6/user/space")
    fun getUserSpace(
        @Query("uid") uid: String,
    ): Call<FeedContentResponse>

    @GET("/v6/user/feedList?showAnonymous=0&isIncludeTop=1&showDoing=0")
    fun getUserFeed(
        @Query("uid") uid: String,
        @Query("page") page: Int,
        @Query("lastItem") lastItem: String?
    ): Call<HomeFeedResponse>

    @GET("/v6/apk/detail")
    fun getAppInfo(
        @Query("id") id: String,
        @Query("installed") installed: Int = 1,
    ): Call<FeedContentResponse>

    @POST("/v6/apk/download?extra=")
    fun getAppDownloadLink(
        @Query("pn") id: String,
        @Query("aid") aid: String,
        @Query("vc") vc: String,
    ): Call<Any>

    @Multipart
    @POST("/v6/apk/checkUpdate?coolmarket_beta=0")
    fun getAppsUpdate(
        @Part pkgs: MultipartBody.Part
    ): Call<List<HomeFeedResponse.Data>>

    @GET //("/v6/topic/newTagDetail")
    fun getTopicLayout(
        @Url url: String,
        @Query("tag") tag: String?, // topic
        @Query("id") id: String? // product
    ): Call<FeedContentResponse>

    @GET("/v6/product/detail")
    fun getProductLayout(
        @Query("id") id: String
    ): Call<FeedContentResponse>

    @GET("/v6/user/profile")
    fun getProfile(
        @Query("uid") uid: String
    ): Call<FeedContentResponse>

    @GET
    fun getFollowList(
        @Url url: String,
        @Query("uid") uid: String?,
        @Query("id") id: String?,
        @Query("showDefault") showDefault: Int?,
        @Query("page") page: Int,
        @Query("lastItem") lastItem: String?
    ): Call<HomeFeedResponse>

    @POST
    fun postLike(
        @Url url: String,
        @Query("id") id: String
    ): Call<LikeResponse>

    @GET("/v6/account/checkLoginInfo")
    fun checkLoginInfo(
    ): Call<CheckResponse>

    @GET
    fun getLoginParam(
        @Url url: String
    ): Call<ResponseBody>

    @POST("/auth/loginByCoolApk")
    @FormUrlEncoded
    fun tryLogin(@FieldMap data: HashMap<String, String?>): Call<ResponseBody>

    @GET
    fun getCaptcha(@Url url: String): Call<ResponseBody>

    @GET
    fun getValidateCaptcha(@Url url: String): Call<ResponseBody>

    @POST("v6/feed/reply")
    @FormUrlEncoded
    fun postReply(
        @FieldMap data: HashMap<String, String>,
        @Query("id") id: String,
        @Query("type") type: String
    ): Call<FeedContentResponse>

    @GET("/v6/page/dataList")
    fun getDataList(
        @Query("url") url: String,
        @Query("title") title: String,
        @Query("subTitle") subTitle: String?,
        @Query("lastItem") lastItem: String?,
        @Query("page") page: Int
    ): Call<HomeFeedResponse>

    @GET("/v6/dyhArticle/list")
    fun getDyhDetail(
        @Query("dyhId") dyhId: String,
        @Query("type") type: String,
        @Query("page") page: Int,
        @Query("lastItem") lastItem: String?
    ): Call<HomeFeedResponse>

    @GET("/v6/picture/list")
    fun getCoolPic(
        @Query("tag") tag: String,
        @Query("type") type: String,
        @Query("page") page: Int,
        @Query("lastItem") lastItem: String?
    ): Call<HomeFeedResponse>


    @GET("/auth/login")
    fun getSmsLoginParam(
        @Query("type") type: String = "mobile",
    ): Call<ResponseBody>

    @POST("/auth/login")
    @FormUrlEncoded
    fun getSmsToken(
        @Query("type") type: String = "mobile",
        @FieldMap data: HashMap<String, String?>
    ): Call<ResponseBody>

    @GET
    fun getMessage(
        @Url url: String,
        @Query("page") page: Int,
        @Query("lastItem") lastItem: String?
    ): Call<HomeFeedResponse>

    @POST
    fun postFollowUnFollow(
        @Url url: String,
        @Query("uid") uid: String
    ): Call<LikeResponse>

    @POST("/v6/feed/createFeed")
    @FormUrlEncoded
    fun postCreateFeed(
        @FieldMap data: HashMap<String, String>
    ): Call<CreateFeedResponse>

    @POST("/v6/account/requestValidate")
    @FormUrlEncoded
    fun postRequestValidate(
        @FieldMap data: HashMap<String, String?>
    ): Call<LikeResponse>

    @GET("/v6/vote/commentList")
    fun getVoteComment(
        @Query("fid") fid: String,
        @Query("extra_key") extraKey: String,
        @Query("page") page: Int,
        @Query("firstItem") firstItem: String?,
        @Query("lastItem") lastItem: String?,
    ): Call<TotalReplyResponse>

    @GET("/v6/question/answerList")
    fun getAnswerList(
        @Query("id") fid: String,
        @Query("sort") sort: String,
        @Query("page") page: Int,
        @Query("firstItem") firstItem: String?,
        @Query("lastItem") lastItem: String?,
    ): Call<TotalReplyResponse>

    @GET
    fun getProductList(
        @Url url: String
    ): Call<HomeFeedResponse>

    @GET
    fun getCollectionList(
        @Url url: String,
        @Query("uid") uid: String?,
        @Query("id") id: String?,
        @Query("showDefault") showDefault: Int,
        @Query("page") page: Int,
        @Query("lastItem") lastItem: String?
    ): Call<HomeFeedResponse>

    @POST
    fun postDelete(
        @Url url: String,
        @Query("id") id: String,
    ): Call<LikeResponse>

    @POST("/v6/product/changeFollowStatus")
    @FormUrlEncoded
    fun postFollow(
        @FieldMap data: HashMap<String, String>
    ): Call<LikeResponse>

    @GET
    fun getFollow(
        @Url url: String,
        @Query("tag") tag: String?,
        @Query("id") id: String?,
    ): Call<LikeResponse>

    @POST("/v6/upload/ossUploadPrepare")
    @FormUrlEncoded
    fun postOSSUploadPrepare(
        @FieldMap data: HashMap<String, String>
    ): Call<OSSUploadPrepareResponse>

    @GET("/v6/feed/searchTag")
    fun getSearchTag(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("recentIds") recentIds: String?,
        @Query("firstItem") firstItem: String?,
        @Query("lastItem") lastItem: String?,
    ): Call<HomeFeedResponse>

    @GET("/v6/feed/loadShareUrl")
    fun loadShareUrl(
        @Query("url") url: String,
        @Query("packageName") packageName: String = EMPTY_STRING,
    ): Call<LoadUrlResponse>

    @GET
    fun messageOperation(
        @Url url: String,
        @Query("ukey") ukey: String?,
        @Query("uid") uid: String?,
        @Query("page") page: Int?,
        @Query("firstItem") firstItem: String?,
        @Query("lastItem") lastItem: String?,
    ): Call<HomeFeedResponse>

    @Multipart
    @POST("/v6/message/send")
    fun sendMessage(
        @Query("uid") uid: String,
        @Part message: MultipartBody.Part,
        @Part pic: MultipartBody.Part
    ): Call<HomeFeedResponse>

    @GET
    fun deleteMessage(
        @Url url: String,
        @Query("ukey") ukey: String?,
        @Query("id") uid: String?,
    ): Call<LikeResponse>

    @GET("/v6/message/showImage")
    fun getImageUrl(
        @Query("id") id: String,
        @Query("type") type: String = "s",
    ): Call<Any>

    @GET("/v6/notification/checkCount")
    fun checkCount(): Call<CheckCountResponse>

    @POST
    @FormUrlEncoded
    fun postDelete(
        @Url url: String,
        @FieldMap data: Map<String, String>?
    ): Call<LikeResponse>
}
