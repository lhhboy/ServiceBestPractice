package com.lhh.servicebestpractice;

public interface DownloadListener {
    void onProgress(int progress);//下载进度条
    void onSuccess();//通知下载成功
    void onFailed();//下载失败通知
    void onPause();//下载暂停事件
    void onCancelde();//下载取消事件
}
