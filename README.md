Android-HttpDownloadManager
===========================

An useful and effective http/https download manager for Android. This download manager is designed according to the idead and implements of Volley.

Usage
=====
* If you don't set the destination file path, the download manager will use *Environment.DIRECTORY_DOWNLOADS* in SDCard as default directory.

>     DownloadRequest request = new DownloadRequest()
>     		.setDownloadId(downloadId)
>     		.setUrl("http://xxx....")
>     		.setDownloadListener(new DownloadListener() {
>     				@Override
> 					public void onStart(int i, long l) {
> 					}
> 
> 					@Override
> 					public void onRetry(int i) {
> 					}
> 
> 					@Override
> 					public void onProgress(int i, long l, long l2) {
> 					}
> 
> 					@Override
> 					public void onSuccess(int i, String s) {
> 					}
> 
> 					@Override
> 					public void onFailure(int i, int i2, String s) {
> 					});
>     mDownloadManager.add(request);

* You need *android.permission.WRITE_EXTERNAL_STORAGE permission* if you don't use public directory in SDCard as download destination file path.

Credits
-------
  * [Volley][1] - Google networking library for android.

License
=======

    Copyright (C) 2014 Vincent Cheung

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
