package com.juicy.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainModel : ViewModel() {
    val articleList: MutableList<WxArticle> = ArrayList()
    val resultLiveData: MutableLiveData<ReportBo> = MutableLiveData()

    fun getArticleData() {
        newScope {
            getAsync<BaseBo<List<WxArticle>>> { url = "https://wanandroid.com/wxarticle/chapters/json" }
                .getResult({
                    if (it == null) {
                        return@getResult
                    }
                    if (it.isSuccess() && it.data != null) {
                        articleList.addAll(it.data)
                        resultLiveData.postValue(ReportBo(true))
                    } else {
                        resultLiveData.postValue(ReportBo(false, it.errorMsg))
                    }
                }, { resultLiveData.postValue(ReportBo(false, "网络异常")) })
        }
    }
}