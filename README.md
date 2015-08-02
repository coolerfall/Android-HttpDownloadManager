Android-HttpDownloadManager
===========================

An useful and effective http/https download manager for Android. This download manager is designed according to the idea and implementation of Volley.

Usage
=====
* If you don't set the destination file path, the download manager will use `Environment.DIRECTORY_DOWNLOADS` in SDCard as default directory:

>     DownloadManager manager = new DownloadManager();
>     String destPath = Environment.getExternalStorageDirectory() + 
>     				File.separator + "test.apk";
>     DownloadRequest request = new DownloadRequest()
>     		.setDownloadId(downloadId)
>     		.setUrl("http://xxx....")
>     		.setDestFilePath(destPath)
>     		.setDownloadListener(new DownloadListener() {
>     				@Override
> 					public void onStart(int downloadId, long totalBytes) {
> 					}
> 
> 					@Override
> 					public void onRetry(int downloadId) {
> 					}
> 
> 					@Override
> 					public void onProgress(int downloadId, long bytesWritten, long totalBytes) {
> 					}
> 
> 					@Override
> 					public void onSuccess(int downloadId, String filePath) {
> 					}
> 
> 					@Override
> 					public void onFailure(int downloadId, int statusCode, String errMsg) {
> 					});
> 					
>     manager.add(request);

* If you just want to know if downloading was successful or failed, then you can use `setSimpleDownloadListener(SimpleDownloadListener l)` instead.
* You can also set retry time with method `setRetryTime(int retryTime)` if necessary, default retry time is 1.
* This manager support downloading in different network type with method `setAllowedNetworkTypes(Context context, int types)`, the types can be `DownloadRequest.NETWORK_MOBILE` and `DownloadRequest.NETWORK_WIFI`. This method need *android.permission.ACCESS_NETWORK_STATE* permission.
* The thread pool size of download manager is 3 by default. If you need a larger pool, then you can create download manager like this: `DownloadManager manager = new DownloadManager(5);`.
* You need *android.permission.WRITE_EXTERNAL_STORAGE* permission if you don't use public directory in SDCard as download destination file path. Don't forget to add *android.permission.INTERNET* permission.
* This download manager support breakpoint downloading, so you can restart the downloading after pause.

Download
--------
Download [the latest JAR][2] or Gradle:
	
	compile 'com.coolerfall:android-http-download-manager:1.5.1'


Credits
-------
  * [Volley][1] - Google networking library for android.

License
=======

    Copyright (C) 2014-2015 Vincent Cheung

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 


[1]: https://android.googlesource.com/platform/frameworks/volley
[2]: https://search.maven.org/remote_content?g=com.coolerfall&a=android-http-download-manager&v=LATEST
