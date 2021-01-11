package com.renyu.androidalishortvideolibrary

import androidx.multidex.MultiDexApplication
import com.aliyun.common.httpfinal.QupaiHttpFinal
import com.aliyun.svideo.downloader.DownloaderManager
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.fresco.FrescoImageLoader
import com.renyu.commonlibrary.commonutils.ImagePipelineConfigUtils

public class MyApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        QupaiHttpFinal.getInstance().initOkHttpFinal()

        DownloaderManager.getInstance().init(this)

        // 初始化fresco
        BigImageViewer.initialize(
            FrescoImageLoader.with(
                this,
                ImagePipelineConfigUtils.getDefaultImagePipelineConfig(this)
            )
        )

    }
}