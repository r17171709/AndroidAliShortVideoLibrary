/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.svideo.editor;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import androidx.annotation.Nullable;

import com.aliyun.common.utils.ToastUtil;
import com.aliyun.svideo.base.widget.ProgressDialog;
import com.aliyun.svideo.common.utils.ToastUtils;
import com.aliyun.svideo.editor.bean.AlivcEditInputParam;
import com.aliyun.svideo.editor.editor.EditorActivity;
import com.aliyun.svideo.media.MediaInfo;
import com.aliyun.svideosdk.common.AliyunErrorCode;
import com.aliyun.svideosdk.common.struct.common.VideoDisplayMode;
import com.aliyun.svideosdk.common.struct.common.VideoQuality;
import com.aliyun.svideosdk.common.struct.encoder.VideoCodecs;
import com.duanqu.transcode.NativeParser;
import com.renyu.androidalishortvideolibrary.R;
import com.renyu.imagelibrary.commonutils.Utils;

import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 编辑模块的media选择Activity
 */
public class EditorMediaActivity extends Activity {
    // 图片代表的时长
    private static final int IMAGE_DURATION = 3000;

    private ProgressDialog progressDialog;

    private Transcoder mTransCoder;
    private AlivcEditInputParam mInputParam;

    private int mRatio;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        init();
        // 选择图片
        Utils.choicePic(this, 9, 1000);
    }

    private void initData() {
        Intent intent = getIntent();
        int mFrameRate = intent.getIntExtra(AlivcEditInputParam.INTENT_KEY_FRAME, 30);
        int mGop = intent.getIntExtra(AlivcEditInputParam.INTENT_KEY_GOP, 250);
        mRatio = intent.getIntExtra(AlivcEditInputParam.INTENT_KEY_RATION_MODE, AlivcEditInputParam.RATIO_MODE_9_16);
        VideoQuality mVideoQuality = (VideoQuality) intent.getSerializableExtra(AlivcEditInputParam.INTENT_KEY_QUALITY);
        if (mVideoQuality == null) {
            mVideoQuality = VideoQuality.HD;
        }
        int mResolutionMode = intent.getIntExtra(AlivcEditInputParam.INTENT_KEY_RESOLUTION_MODE, AlivcEditInputParam.RESOLUTION_720P);
        VideoCodecs mVideoCodec = (VideoCodecs) intent.getSerializableExtra(AlivcEditInputParam.INTENT_KEY_CODEC);
        if (mVideoCodec == null) {
            mVideoCodec = VideoCodecs.H264_HARDWARE;
        }
        int mCrf = intent.getIntExtra(AlivcEditInputParam.INTETN_KEY_CRF, 23);
        float mScaleRate = intent.getFloatExtra(AlivcEditInputParam.INTETN_KEY_SCANLE_RATE, 1.0f);
        VideoDisplayMode mScaleMode = (VideoDisplayMode) intent.getSerializableExtra(AlivcEditInputParam.INTETN_KEY_SCANLE_MODE);
        if (mScaleMode == null) {
            mScaleMode = VideoDisplayMode.FILL;
        }
        boolean mHasTailAnimation = intent.getBooleanExtra(AlivcEditInputParam.INTENT_KEY_TAIL_ANIMATION, false);
        boolean canReplaceMusic = intent.getBooleanExtra(AlivcEditInputParam.INTENT_KEY_REPLACE_MUSIC, true);
        ArrayList<MediaInfo> mediaInfos = intent.getParcelableArrayListExtra(AlivcEditInputParam.INTENT_KEY_MEDIA_INFO);
        boolean hasWaterMark = intent.getBooleanExtra(AlivcEditInputParam.INTENT_KEY_WATER_MARK, false);
        mInputParam = new AlivcEditInputParam.Builder()
                .setFrameRate(mFrameRate)
                .setGop(mGop)
                .setRatio(mRatio)
                .setVideoQuality(mVideoQuality)
                .setResolutionMode(mResolutionMode)
                .setVideoCodec(mVideoCodec)
                .setCrf(mCrf)
                .setScaleRate(mScaleRate)
                .setScaleMode(mScaleMode)
                .setHasTailAnimation(mHasTailAnimation)
                .setCanReplaceMusic(canReplaceMusic)
                .addMediaInfos(mediaInfos)
                .setHasWaterMark(hasWaterMark)
                .build();
    }

    private void init() {
        mTransCoder = new Transcoder();
        mTransCoder.init(this);
        mTransCoder.setTransCallback(new Transcoder.TransCallback() {
            @Override
            public void onError(Throwable e, final int errorCode) {
                runOnUiThread(() -> {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    switch (errorCode) {
                        case AliyunErrorCode.ALIVC_SVIDEO_ERROR_MEDIA_NOT_SUPPORTED_AUDIO:
                            ToastUtil.showToast(EditorMediaActivity.this, R.string.alivc_crop_video_tip_not_supported_audio);
                            break;
                        case AliyunErrorCode.ALIVC_COMMON_UNKNOWN_ERROR_CODE:
                        case AliyunErrorCode.ALIVC_SVIDEO_ERROR_MEDIA_NOT_SUPPORTED_VIDEO:
                        default:
                            ToastUtil.showToast(EditorMediaActivity.this, R.string.alivc_crop_video_tip_crop_failed);
                    }
                });

            }

            @Override
            public void onProgress(int progress) {
                if (progressDialog != null) {
                    progressDialog.setProgress(progress);
                }
            }

            @Override
            public void onComplete(List<MediaInfo> resultVideos) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                mInputParam.setMediaInfos((ArrayList<MediaInfo>) resultVideos);
                EditorActivity.startEdit(EditorMediaActivity.this, mInputParam);
            }

            @Override
            public void onCancelComplete() {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            ArrayList<Uri> choiceImages = data.getParcelableArrayListExtra("choiceImages");
            if (choiceImages.size() > 0) {
                for (int i = 0; i < choiceImages.size(); i++) {
                    MediaInfo mediaInfo1 = new MediaInfo();
                    mediaInfo1.filePath = choiceImages.get(i).getPath();
                    mediaInfo1.fileUri = choiceImages.get(i).toString();
                    mediaInfo1.mimeType = "image/*";
                    add(mediaInfo1);
                }
            }
            onNext();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTransCoder.release();
    }

    private static class OnCancelListener implements DialogInterface.OnCancelListener {
        private WeakReference<EditorMediaActivity> weakReference;

        private OnCancelListener(EditorMediaActivity mediaActivity) {
            weakReference = new WeakReference<>(mediaActivity);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            EditorMediaActivity mediaActivity = weakReference.get();
            if (mediaActivity != null) {
                // 为了防止未取消成功的情况下就开始下一次转码，这里在取消转码成功前会禁用下一步按钮
                mediaActivity.mTransCoder.cancel();
            }
        }
    }

    public void add(MediaInfo info) {
        MediaInfo infoCopy = new MediaInfo();
        infoCopy.mimeType = info.mimeType;
        if (info.mimeType.startsWith("image")) {
            if (info.filePath.endsWith("gif") || info.filePath.endsWith("GIF")) {
                NativeParser parser = new NativeParser();
                parser.init(info.filePath);
                int frameCount;

                try {
                    frameCount = Integer.parseInt(parser.getValue(NativeParser.VIDEO_FRAME_COUNT));
                } catch (Exception e) {
                    ToastUtils.show(EditorMediaActivity.this, R.string.alivc_editor_error_tip_play_video_error);
                    parser.release();
                    parser.dispose();
                    return;
                }
                //当gif动图为一帧的时候当作图片处理，否则当作视频处理
                if (frameCount > 1) {
                    int duration;
                    try {
                        duration = Integer.parseInt(parser.getValue(NativeParser.VIDEO_DURATION)) / 1000;
                    } catch (Exception e) {
                        ToastUtils.show(EditorMediaActivity.this, R.string.alivc_editor_error_tip_play_video_error);
                        parser.release();
                        parser.dispose();
                        return;
                    }
                    infoCopy.mimeType = "video";
                    infoCopy.duration = duration;
                } else {
                    infoCopy.duration = IMAGE_DURATION;
                }
                parser.release();
                parser.dispose();
            } else {
                if (mRatio == AlivcEditInputParam.RATIO_MODE_ORIGINAL) {
                    //原比例下android解码器对图片大小有要求，目前支持为单边不大于3840
                    try {
                        ParcelFileDescriptor pfd = EditorMediaActivity.this.getContentResolver().openFileDescriptor(Uri.parse(info.fileUri), "r");
                        if (pfd != null) {
                            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                            if (bitmap != null && (bitmap.getHeight() > 3840 || bitmap.getWidth() > 3840)) {
                                ToastUtils.show(EditorMediaActivity.this, "原尺寸输出时，图片宽高不能超过3840");
                                return;
                            }
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                infoCopy.duration = IMAGE_DURATION;
            }
        } else {
            infoCopy.duration = info.duration;
        }
        infoCopy.filePath = info.filePath;
        infoCopy.fileUri = info.fileUri;
        mTransCoder.addMedia(infoCopy);
    }

    private void onNext() {
        //对于大于720P的视频需要走转码流程
        int videoCount = mTransCoder.getVideoCount();
        if (videoCount > 0 && (progressDialog == null || !progressDialog.isShowing())) {
            progressDialog = ProgressDialog.show(EditorMediaActivity.this, null, getResources().getString(R.string.alivc_media_wait));
            progressDialog.setCancelable(true);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setOnCancelListener(new OnCancelListener(EditorMediaActivity.this));
            mTransCoder.transcode(EditorMediaActivity.this.getApplicationContext(), mInputParam.getScaleMode());
        } else {
            ToastUtil.showToast(EditorMediaActivity.this, R.string.alivc_media_please_select_video);
        }
    }

    public static void startImport(Context context, AlivcEditInputParam param) {
        if (param == null) {
            return;
        }
        Intent intent = new Intent(context, EditorMediaActivity.class);
        intent.putExtra(AlivcEditInputParam.INTENT_KEY_FRAME, param.getFrameRate());
        intent.putExtra(AlivcEditInputParam.INTENT_KEY_GOP, param.getGop());
        intent.putExtra(AlivcEditInputParam.INTENT_KEY_RATION_MODE, param.getRatio());
        intent.putExtra(AlivcEditInputParam.INTENT_KEY_QUALITY, param.getVideoQuality());
        intent.putExtra(AlivcEditInputParam.INTENT_KEY_RESOLUTION_MODE, param.getResolutionMode());
        intent.putExtra(AlivcEditInputParam.INTENT_KEY_CODEC, param.getVideoCodec());
        intent.putExtra(AlivcEditInputParam.INTETN_KEY_CRF, param.getCrf());
        intent.putExtra(AlivcEditInputParam.INTETN_KEY_SCANLE_RATE, param.getScaleRate());
        intent.putExtra(AlivcEditInputParam.INTETN_KEY_SCANLE_MODE, param.getScaleMode());
        intent.putExtra(AlivcEditInputParam.INTENT_KEY_TAIL_ANIMATION, param.isHasTailAnimation());
        intent.putExtra(AlivcEditInputParam.INTENT_KEY_REPLACE_MUSIC, param.isCanReplaceMusic());
        intent.putExtra(AlivcEditInputParam.INTENT_KEY_WATER_MARK, param.isHasWaterMark());
        intent.putParcelableArrayListExtra(AlivcEditInputParam.INTENT_KEY_MEDIA_INFO, param.getMediaInfos());
        context.startActivity(intent);
    }
}
