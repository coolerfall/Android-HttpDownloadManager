# Quick Start

* Add the following in your `build.gradle` file:

```gradle
implementation 'com.coolerfall:android-http-download-manager:2.0.0'
```

* Now download a file with this:

```java
DownloadManager manager = new DownloadManager.Builder()
    .context(this)
    .build();

DownloadRequest request = new DownloadRequest.Builder()
    .url("http://something.to.download")
    .build();

int downloadId = manager.add(request);
```

For more details, see [configuration](/configuration)