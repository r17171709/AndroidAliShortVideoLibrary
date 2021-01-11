/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.svideo.editor.editor;

import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.renyu.androidalishortvideolibrary.R;
import com.aliyun.svideo.editor.editor.thumblinebar.OverlayThumbLineBar;
import com.aliyun.svideo.editor.effects.control.UIEditorPage;
import com.aliyun.svideo.editor.widget.BaseAliyunPasterView;
import com.aliyun.svideosdk.common.struct.effect.ActionBase;
import com.aliyun.svideosdk.common.struct.effect.ActionTranslate;
import com.aliyun.svideosdk.editor.AliyunIEditor;
import com.aliyun.svideosdk.editor.AliyunPasterController;

public class PasterUIGifImpl extends AbstractPasterUISimpleImpl {
    protected AliyunIEditor mAliyunIEditor;

    public PasterUIGifImpl(BaseAliyunPasterView pasterView, AliyunPasterController controller, OverlayThumbLineBar thumbLineBar) {
        super(pasterView, controller, thumbLineBar);
        mEditorPage = UIEditorPage.OVERLAY;
        mText = pasterView.getContentView().findViewById(R.id.qupai_overlay_content_text);
        int width = controller.getPasterWidth();
        int height = controller.getPasterHeight();

        mPasterView.setContentWidth(width);
        mPasterView.setContentHeight(height);
        mirrorPaster(mController.isPasterMirrored());
        mPasterView.rotateContent(mController.getPasterRotation());
    }
    public PasterUIGifImpl(BaseAliyunPasterView pasterView, AliyunPasterController controller, OverlayThumbLineBar thumbLineBar, AliyunIEditor iEditor) {
        this(pasterView, controller, thumbLineBar);
        this.mAliyunIEditor = iEditor;
    }

    @Override
    public void moveToCenter() {
        mMoveDelay = true;
        mPasterView.post(new Runnable() {
            @Override
            public void run() {
                int cx = mController.getPasterCenterX();
                int cy = mController.getPasterCenterY();
                int pcx = ((ViewGroup)mPasterView.getParent()).getWidth();
                int pcy = ((ViewGroup)mPasterView.getParent()).getHeight();
                mPasterView.moveContent(cx - pcx / 2, cy - pcy / 2);
            }
        });
    }

    @Override
    public int getPasterWidth() {
        float[] scale = mPasterView.getScale();
        float scaleX = scale[0];
        int width = mPasterView.getContentWidth();
        return (int)(width * scaleX);
    }

    @Override
    public int getPasterHeight() {
        float[] scale = mPasterView.getScale();
        float scaleY = scale[1];
        int height = mPasterView.getContentHeight();
        return (int)(height * scaleY);
    }

    @Override
    public int getPasterCenterY() {
        if (mMoveDelay) {
            return 0;
        }
        float[] center = mPasterView.getCenter();
        if (center == null) {
            return 0;
        }
        float y = center[1];
        return (int)(y);
    }

    @Override
    public int getPasterCenterX() {
        if (mMoveDelay) {
            return 0;
        }
        float[] center = mPasterView.getCenter();
        if (center == null) {
            return 0;
        }
        float x = center[0];
        return (int)(x);
    }

    @Override
    public void mirrorPaster(boolean mirror) {
        super.mirrorPaster(mirror);
        animPlayerView.setMirror(mirror);
    }

    @Override
    protected void playPasterEffect() {
        TextureView pv = new TextureView(mPasterView.getContext());
        animPlayerView = mController.createPasterPlayer(pv);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        ViewGroup vg = (ViewGroup) mPasterView.getContentView();
        vg.addView(pv, lp);
    }

    @Override
    protected void stopPasterEffect() {
        ViewGroup vg = (ViewGroup) mPasterView.getContentView();
        vg.removeAllViews();
        animPlayerView = null;
    }

    @Override
    public float getPasterRotation() {
        return mPasterView.getRotation();
    }

    @Override
    public View getPasterView() {
        return mPasterView;
    }

    /**
     * 目前只有字幕支持动效
     */
    @Override
    public void editTimeCompleted() {
        if (!isEditStarted || !isPasterExists() || isPasterRemoved()) {
            return;
        }
        super.editTimeCompleted();
        if (mAliyunIEditor != null) {
            if (mOldFrameAction != null) {
                mAliyunIEditor.removeFrameAnimation(mOldFrameAction);
                mOldFrameAction = null;
            }
            mFrameAction = mTempFrameAction;

            if (mFrameAction != null) {
                applyAnimation(mFrameAction, this instanceof PasterUITextImpl);
            }  else {
                mAliyunIEditor.addAnimationFilter(null);
            }
            mOldFrameAction = mFrameAction;
        }
        isEditStarted = false;
    }

    private void applyAnimation(ActionBase actionBase, boolean isTextView) {

        ActionTranslate actionTranslate = null;
        if (actionBase instanceof ActionTranslate) {
            actionTranslate = new ActionTranslate();
        }

        long pasterStartTime = mController.getPasterStartTime();
        //long pasterDuration = mController.getPasterDuration();
        long pasterDuration = 1000 * 1000;

        actionBase.setTargetId(mController.getEffect().getViewId());
        actionBase.setStartTime(pasterStartTime);
        actionBase.setDuration(pasterDuration);
        if (actionTranslate != null) {
            actionTranslate.setTargetId(mController.getEffect().getViewId());
            actionTranslate.setStartTime(pasterStartTime);
            actionTranslate.setDuration(pasterDuration);
            setTranslateParams(actionBase, actionTranslate, isTextView);
            mAliyunIEditor.addFrameAnimation(actionTranslate);
            mFrameAction = actionTranslate;
        } else {
            mAliyunIEditor.addFrameAnimation(actionBase);
        }

    }
    /**
     * 因为dialog中无法获取准确的位移位置，需要在这里对位移参数重新设定
     * @param actionBase ActionBase
     * @param isTextView 是不是操作textView
     */
    private void setTranslateParams(ActionBase animation, ActionBase actionBase, boolean isTextView) {

        ActionTranslate actionTranslate = (ActionTranslate) actionBase;
        ViewParent parent = mPasterView.getParent();
        if (parent == null) {
            return;
        }

        float toPointX = ((ActionTranslate) animation).getToPointX();
        float toPointY = ((ActionTranslate) animation).getToPointY();
        float widthUnit = mPasterView.getWidth() / 2f;
        float heightUnit = mPasterView.getHeight() / 2f;
        float left = mPasterView.getContentView().getLeft();
        float right = mPasterView.getContentView().getRight();
        float top = mPasterView.getContentView().getTop();
        float bottom = mPasterView.getContentView().getBottom();

        float x = ((right + left) / 2 - widthUnit) / widthUnit;
        float y = -((top + bottom) / 2 - heightUnit) / heightUnit;
        float detY = 0;
        if (isTextView) {
            int paddingTop = mText.getPaddingTop();
            detY = (-(top + mText.getTextHeight() / 2 + paddingTop - heightUnit) + ((top + bottom) / 2 - heightUnit)) / heightUnit;
            y = -(top + mText.getTextHeight() / 2 + paddingTop - heightUnit) / heightUnit;
        }

        //出场
//        actionTranslate.setFromPointX(x);
//        actionTranslate.setFromPointY(y);
//        if (toPointX == 1f){
//            //向右平移
//            actionTranslate.setToPointY(y);
//            actionTranslate.setToPointX(1);
//        }else if (toPointX == -1f){
//            //向左平移
//            actionTranslate.setToPointY(y);
//            actionTranslate.setToPointX(-1);
//        }else if (toPointY == -1f){
//            //向下平移
//            actionTranslate.setToPointX(x);
//            actionTranslate.setToPointY(-1 + detY);
//        }else if (toPointY == 1f){
//            //向上平移
//            actionTranslate.setToPointX(x);
//            actionTranslate.setToPointY(1 + detY);
//        }

        //入场1s结束
        actionTranslate.setToPointX(x);
        actionTranslate.setToPointY(y);
        if (toPointX == 1f) {
            //向右平移
            actionTranslate.setFromPointY(y);
            actionTranslate.setFromPointX(-1);
        } else if (toPointX == -1f) {
            //向左平移
            actionTranslate.setFromPointY(y);
            actionTranslate.setFromPointX(1);
        } else if (toPointY == -1f) {
            //向下平移
            actionTranslate.setFromPointX(x);
            actionTranslate.setFromPointY(1 + detY);
        } else if (toPointY == 1f) {
            //向上平移
            actionTranslate.setFromPointX(x);
            actionTranslate.setFromPointY(-1f + detY);
        }

    }



    /**
     * 拷贝动画
     * @return ActionBase
     */
    private ActionBase copyFrameAction() {
        ActionBase actionBase = new ActionBase();
        actionBase.setDuration(mFrameAction.getDuration());
        //actionBase.set
        return null;
    }
}
