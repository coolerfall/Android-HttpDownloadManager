
Android Http Download Manager
===========================

An useful and effective http download manager for Android. This download manager is designed to adapt different http library.


Usage
=====
* After android Q, we can just save files in external private directory and public download directory, so the download manager will use `Context#getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)` as default directory. It will detect filename automatically from header or url if `relativeFilePath` was not set:
```java
DownloadManager manager = new DownloadManager.Builder().context(this)
        .downloader(OkHttpDownloader.create(client))
        .threadPoolSize(3)
        .logger(new Logger() {
          @Override public void log(String message) {
            Log.d("TAG", message);
          }
        })
        .build();

DownloadRequest request = 
          new DownloadRequest.Builder()
              .url("http://something.to.download")
              .retryTime(5)
              .retryInterval(2, TimeUnit.SECONDS)
              .progressInterval(1, TimeUnit.SECONDS)
              .priority(Priority.HIGH)
              .relativeFilePath("somedir/test.apk")
              .downloadCallback(new DownloadCallback() {
                  @Override public void onStart(int downloadId, long totalBytes) {
						
                  }

                  @Override public void onRetry(int downloadId) {
						
                  }

                  @Override
                  public void onProgress(int downloadId, long bytesWritten, long totalBytes) {
						
                  }

                  @Override public void onSuccess(int downloadId, String filePath) {
						
                  }

                  @Override public void onFailure(int downloadId, int statusCode, String errMsg) {
						
                  }
              })
              .build();
				
int downloadId = manager.add(request);
```
It's easy to stop:
```java
	/* stop single */
	manager.cancel(downloadId);
	/* stop all */
	manager.cancelAll();
```

* If you don't want to set the filename but want to set the download directory, then you can use `relativeDirectory(String directory)`, but this method will be ignored if `relativeFilePath((String filePath)` was used.
* You can also set retry time with method `retryTime(int retryTime)` if necessary, default retry time is 1. You can set retry interval to decide how long to retry with method `retryInterval(long interval, TimeUnit unit)`.
* The thread pool size of download manager is 3 by default. If you need a larger pool, then you can try the method `threadPoolSize(int poolSize)` in `DownloadManager#Builder`.
* This download manager support breakpoint downloading, so you can restart the downloading after pause.
* If you don't want DownloadDispathcer invoke `onProgress(int downloadId, long bytesWritten, long totalBytes)` frequently, then you can use `progressInterval(long interval, TimeUnit unit)`.
* If you want one download request get high priority, then you can use `priority(Priority priority)`.
* The download manager provides two kinds of `Downloader`(`URLDownloader` and `OkHttpDownloader`), and the it will detect which downloader to use. You can also implement your own `Downloader` just like what `URLDownloader` and `OkHttpDownloader` do.
* If you want to copy files to external public download directory, `DownloadManager` provides `copyToPublicDownloadDir(String filepath)`.



Download
========

	implementation 'com.coolerfall:android-http-download-manager:2.0.0-SNAPSHOT'

Note
====
If you're using `OkHttpDownloader` with custom `OkHttpClient` as `Downloader` in `DownloadManager`, then you should not add [HttpLoggingInterceptor][2] in your custom `OkHttpClient`. It may be crashed(OOM) as `HttpLoggingInterceptor ` use `okio` to reqeust the whole body in memory.


Supports
=======
If you want to support this project, you can buy a coffee.

<a href="https://www.buymeacoffee.com/coolerfall" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;" ></a>


License
=======

    Copyright (C) 2014-2022 Vincent Cheung
    
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
[2]: https://github.com/square/okhttp/tree/master/okhttp-logging-interceptor
[3]: https://search.maven.org/remote_content?g=com.coolerfall&amp;a=android-http-download-manager&amp;v=LATEST
