
ScrollZoomListView
===============

A grace comic reader widget, expand ListView.


Screenshots
-----
<a href="gif/comic01.gif"><img src="gif/comic01.gif" width="30%"/></a>
<a href="gif/comic02.gif"><img src="gif/comic02.gif" width="30%"/></a>
<a href="gif/comic03.gif"><img src="gif/comic03.gif" width="30%"/></a>


ScrollZoomListView use **Animation** and **Canvas** let ListView **scale** and **translate**, add **ScaleGestureDetector** , **GestureDetector** expand ListView.

[![](https://jitpack.io/v/ruzhan123/ScollZoomListView.svg)](https://jitpack.io/#ruzhan123/ScollZoomListView)


Gradle
------

Add it in your root build.gradle at the end of repositories:


```java

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

Add the dependency:


```java

	dependencies {
	        implementation 'com.github.ruzhan123:ScollZoomListView:v1.0'
	}
```


Usage
-----
```xml
<zhan.scrollzoomlist.ScrollZoomListView
    android:id="@+id/scrollZoomListView"
    app:min_zoom_scale="0.4"
    app:max_zoom_scale="2.0"
    app:zoom_scale_duration="300"
    app:zoom_to_small_scale_duration="500"
    app:zoom_to_small_times="6"
    app:normal_scale="1.0"
    app:zoom_scale="2.0"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```

Developed by
-------

 ruzhan - <a href='javascript:'>dev19921116@gmail.com</a>

License
-------

    Copyright 2017 ruzhan

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
