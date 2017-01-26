
ScrollZoomListView
===============

A grace comic reader widget, expand ListView.


expand
-----
**1: scale item:**

![](https://github.com/ruzhan123/ScollZoomListView/raw/master/gif/comic01.gif)




**2: scale and translate total:**

![](https://github.com/ruzhan123/ScollZoomListView/raw/master/gif/comic02.gif)



**3: zoom total:**


![](https://github.com/ruzhan123/ScollZoomListView/raw/master/gif/comic03.gif)



ScrollZoomListView use **Animation** and **Canvas** let ListView **scale** and **translate**, add **ScaleGestureDetector** , **GestureDetector** expand ListView.


Core code
------

```

	  @Override protected void dispatchDraw(@NonNull Canvas canvas) {
	    canvas.save(Canvas.MATRIX_SAVE_FLAG);
	    canvas.translate(mTranslateX, mTranslateY);
	    canvas.scale(mScaleFactor, mScaleFactor);
	    super.dispatchDraw(canvas);
	    canvas.restore();
	  }


```

Gradle
------

Add it in your root build.gradle at the end of repositories:


```

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

```

Add the dependency:


```

	dependencies {
	        compile 'com.github.ruzhan123:ScollZoomListView:v1.0'
	}

```


Usage
-----
```xml
<zhan.scrollzoomlist.ScrollZoomListView
    android:id="@+id/list"
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


expand Interface
-----

Synchronous ListView Animation:


```


	public interface OnListViewZoomListener {
	
		void onListViewZoomUpdate(ValueAnimator animation, float translateX, float translateY,
		    float scaleX, float scaleY);
		
		void onListViewStart();
		
		void onListViewCancel();
	}


	  private List<OnListViewZoomListener> mOnListViewZoomListeners = new ArrayList<>();


```



Synchronous ListView Zoom ScaleGestureDetector:

```

		private List<ScaleGestureDetector.SimpleOnScaleGestureListener> mOnScaleGestureListeners = new ArrayList<>();


```


Synchronous ListView Zoom GestureDetector:

```

	  private List<GestureDetector.SimpleOnGestureListener> mSimpleOnGestureListeners = new ArrayList<>();


```


```



	  public void addOnScaleGestureListener(ScaleGestureDetector.SimpleOnScaleGestureListener listener) {
	    if(listener != null) {
	      if(!mOnScaleGestureListeners.contains(listener)) {
	        mOnScaleGestureListeners.add(listener);
	      }
	    }
	  }
	
	  public void removeOnScaleGestureListener(ScaleGestureDetector.SimpleOnScaleGestureListener listener) {
	    if(listener != null) {
	      if(mOnScaleGestureListeners.contains(listener)) {
	        mOnScaleGestureListeners.remove(listener);
	      }
	    }
	  }



```


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
