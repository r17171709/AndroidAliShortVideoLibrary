package com.aliyun.svideo.editor.msg;

public interface MessageHandler {
    <T> int onHandleMessage(T message);
}
