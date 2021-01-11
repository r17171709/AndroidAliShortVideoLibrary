package com.aliyun.svideo.editor.publish;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aliyun.common.utils.ToastUtil;
import com.aliyun.svideo.base.Constants;
import com.aliyun.svideo.common.utils.DateTimeUtils;
import com.aliyun.svideo.common.utils.ThreadUtils;
import com.aliyun.svideo.common.utils.UriUtils;
import com.aliyun.svideosdk.common.AliyunErrorCode;
import com.aliyun.svideosdk.editor.AliyunIComposeCallBack;
import com.aliyun.svideosdk.editor.AliyunIVodCompose;
import com.duanqu.transcode.NativeParser;
import com.renyu.androidalishortvideolibrary.R;

import java.util.LinkedHashMap;

/**
 * Created by macpro on 2017/11/6.
 * 视频合成页面
 */

public class PublishActivity extends AppCompatActivity {
    public static final String KEY_PARAM_CONFIG = "project_json_path";
    public static final String KEY_PARAM_THUMBNAIL = "svideo_thumbnail";
    public static final String KEY_PARAM_VIDEO_RATIO = "key_param_video_ratio";
    public static final String KEY_PARAM_VIDEO_WIDTH = "key_param_video_width";
    public static final String KEY_PARAM_VIDEO_HEIGHT = "key_param_video_height";

    private String mOutputPath = "";

    private AliyunIVodCompose mCompose;
    private boolean mComposeCompleted;
    private AsyncTask<String, Void, Bitmap> mAsyncTaskResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String config = getIntent().getStringExtra(KEY_PARAM_CONFIG);

        mCompose = ComposeFactory.INSTANCE.getAliyunVodCompose();
        mCompose.init(this.getApplicationContext());

        //开始合成
        String time = DateTimeUtils.getDateTimeFromMillisecond(System.currentTimeMillis());
        mOutputPath = Constants.SDCardConstants.getDir(this) + time + Constants.SDCardConstants.COMPOSE_SUFFIX;
        int ret = mCompose.compose(config, mOutputPath, mCallback);
        if (ret != AliyunErrorCode.ALIVC_COMMON_RETURN_SUCCESS) {
            return;
        }
    }

    @Override
    public void onBackPressed() {
        if (mComposeCompleted) {
            super.onBackPressed();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final AlertDialog dialog = builder.setTitle(R.string.alivc_editor_publish_dialog_cancel_content_tip)
                    .setNegativeButton(R.string.alivc_editor_publish_goback, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mComposeCompleted) {
                                finish();
                            } else {
                                if (mCompose != null) {
                                    mCompose.cancelCompose();
                                }
                                finish();
                            }
                        }
                    })
                    .setPositiveButton(R.string.alivc_editor_publish_continue, null).create();
            dialog.show();
        }
    }

    private final AliyunIComposeCallBack mCallback = new AliyunIComposeCallBack() {
        @Override

        public void onComposeError(int errorCode) {
            runOnUiThread(() -> ToastUtil.showToast(PublishActivity.this, R.string.alivc_editor_publish_compose_failed));
        }

        @Override
        public void onComposeProgress(final int progress) {
            runOnUiThread(() -> Log.d("TAGTAGTAG", progress + "%"));
        }

        @Override
        public void onComposeCompleted() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //适配android Q
                ThreadUtils.runOnSubThread(() -> UriUtils.saveVideoToMediaStore(PublishActivity.this, mOutputPath));

            } else {
                MediaScannerConnection.scanFile(getApplicationContext(),
                        new String[]{mOutputPath}, new String[]{"video/mp4"}, null);
            }
            mComposeCompleted = true;
            runOnUiThread(() -> {
                //请务必在非回调线程调用，避免内存泄露
                if (mCompose != null) {
                    mCompose.release();
                    mCompose = null;
                }
            });
            printVideoInfo(mOutputPath);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (mCompose != null) {
            mCompose.resumeCompose();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCompose != null) {
            mCompose.pauseCompose();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mCompose != null) {
            mCompose.release();
            mCompose = null;
        }
        if (mAsyncTaskResult != null) {
            mAsyncTaskResult.cancel(true);
            mAsyncTaskResult = null;
        }
    }

    public static void printVideoInfo(String outputPath) {
        NativeParser nativeParser = new NativeParser();
        try {
            nativeParser.init(outputPath);
            LinkedHashMap<String, Object> infoMap = new LinkedHashMap<>();
            int height = Integer.parseInt(nativeParser.getValue(NativeParser.VIDEO_HEIGHT));
            int width = Integer.parseInt(nativeParser.getValue(NativeParser.VIDEO_WIDTH));
            int gop = Integer.parseInt(nativeParser.getValue(NativeParser.VIDEO_GOP));
            int frameCount = Integer.parseInt(nativeParser.getValue(NativeParser.VIDEO_FRAME_COUNT));
            long videoDuration = Long.parseLong(nativeParser.getValue(NativeParser.VIDEO_DURATION));
            long bitrate = Long.parseLong(nativeParser.getValue(NativeParser.VIDEO_BIT_RATE));
            long audioDuration = Long.parseLong(nativeParser.getValue(NativeParser.AUDIO_DURATION));
            infoMap.put("path", outputPath);
            infoMap.put("width", width);
            infoMap.put("height", height);
            infoMap.put("videoDuration", videoDuration);
            infoMap.put("audioDuration", audioDuration);
            infoMap.put("gop", gop);
            infoMap.put("frameRate", (float) frameCount / videoDuration * 1000 * 1000);
            infoMap.put("bitrate", bitrate);
            Log.i("printVideoInfo", infoMap.toString());
        } catch (Exception ex) {
            Log.e("printVideoInfo", ex.getMessage());
        } finally {
            nativeParser.release();
            nativeParser.dispose();
        }
    }
}
