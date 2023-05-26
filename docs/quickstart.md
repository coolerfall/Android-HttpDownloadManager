# Quick Start

* Add the following in your `build.gradle` file:

```gradle
implementation 'com.coolerfall:android-http-download-manager:2.0.0'
```

* Now download a file with this:

```java
import com.coolerfall.download.DownloadManager;
import com.coolerfall.download.DownloadRequest;

DownloadRequest request = new DownloadRequest.Builder()
    .url("http://something.to.download")
    .build();

int downloadId = DownloadManager.get().enqueue(request);
```

For more details, see [configuration](/configuration)