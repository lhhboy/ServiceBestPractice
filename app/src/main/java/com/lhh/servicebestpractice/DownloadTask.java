package com.lhh.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Integer,Integer> {
    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;
    private DownloadListener listener;
    private boolean isCanceled = false;
    private boolean isPaused = false;
    private int lastProgress;

    public DownloadTask (DownloadListener listener){
        this.listener=listener;
    }
//在子线程执行的
    @Override
    protected Integer doInBackground(String... params) {
        InputStream is = null;
        //随机访问文件
        RandomAccessFile savedFile = null;
        File file = null;
        try {
        long downloadedLength = 0;//记录已经下载的文件长度
        String downloadUrl = params[0];//获取下载的URL地址
        //根据Url地址解析出下载的文件名
        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
        //将文件下载到SD卡的Download目录，指定下载路径
        String directory = Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_DOWNLOADS).getPath();
        //拼接
        file = new File(directory+fileName);
            Log.d("路径", String.valueOf(file));
        //判断Download目录是否已经存在要下载的文件，存在就读取已下载的字节数，就可以在后面启用断点续传的功能
        if(file.exists()){
            downloadedLength = file.length();
        }

            //通过getContentLength（）方法获取待下载文件总长度
            long contenLength = getContentLength(downloadUrl);
            if(contenLength == 0) {
                return TYPE_FAILED;
            }else if(contenLength == downloadedLength){
                //已下载字节和文件总字节相等，说明已经下载完成
                return TYPE_SUCCESS;
            }
            OkHttpClient client = new OkHttpClient();
            Log.d("TAG", "来了");
            Request request = new Request.Builder()
                    //断点下载，指定从哪个字节开始下载
                    .addHeader("RANGE","byte=" + downloadedLength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if(request != null){
                //获取响应体输入流
                is = response.body().byteStream();
                //rw表示读取方式
                savedFile = new RandomAccessFile(file,"rw");
                savedFile.seek(downloadedLength);//跳过已下载字节
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while ((len=is.read(b))!= -1){
                    if(isCanceled){
                        return TYPE_CANCELED ;
                    }else  if(isPaused){
                        return TYPE_PAUSED ;
                    }else {
                        total +=len;
                        savedFile.write(b,0,len);
                        //计算下载的百分比
                        int progress = (int) ((total + downloadedLength)*100/contenLength);
                                publishProgress(progress);
                    }
                }
                response.close();
                return TYPE_SUCCESS ;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
            if(is !=null){
                    is.close();
                }
            if(savedFile != null){
                savedFile.close();
            }
            if(isCanceled && file !=null){
                file.delete();
            }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }
//后台执行完毕通过return语句进行返回时调用。
    @Override
    protected void onPostExecute(Integer status) {
        switch (status){
            case TYPE_SUCCESS :
                listener.onSuccess();
                break;
            case TYPE_FAILED :
                listener.onFailed();
                break;
            case TYPE_CANCELED :
                listener.onCancelde();
                break;
            case TYPE_PAUSED :
                listener.onPause();
                break;
                default:
                    break;
        }
    }
public void pauseDownload(){
        isPaused = true;
}
public void cancelDownload(){
        isCanceled = true;
}
    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if(progress > lastProgress){
            listener.onProgress(progress);
            lastProgress=progress;
        }
    }

    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client =new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if(response != null && response.isSuccessful()){
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }
}
