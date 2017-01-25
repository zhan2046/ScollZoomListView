package zhan.scollzoomlistview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import zhan.scrollzoomlist.ScrollZoomListView;

public class MainActivity extends AppCompatActivity {

  private List<Integer> mData = new ArrayList<>();
  private ScrollZoomListView list;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    //设置无标题
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    //设置全屏
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);


    setContentView(R.layout.activity_main);

    mData.clear();
    mData.add(R.mipmap.comic1);
    mData.add(R.mipmap.comic2);
    mData.add(R.mipmap.comic3);
    mData.add(R.mipmap.comic4);
    mData.add(R.mipmap.comic5);
    mData.add(R.mipmap.comic6);
    mData.add(R.mipmap.comic7);
    mData.add(R.mipmap.comic8);
    mData.add(R.mipmap.comic9);
    mData.add(R.mipmap.comic10);

    list = (ScrollZoomListView) findViewById(R.id.list);

    MyAdapter adapter = new MyAdapter();
    adapter.setData(mData);
    list.setAdapter(adapter);
  }

  private class MyAdapter extends BaseAdapter {

    private List<Integer> mData;

    public void setData(List<Integer> data) {
      mData = data;
      notifyDataSetChanged();
    }

    @Override public int getCount() {
      return mData == null ? 0 : mData.size();
    }

    @Override public Object getItem(int position) {
      return mData.get(position);
    }

    @Override public long getItemId(int position) {
      return position + 1000;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {

      MyHolder holder;

      if (convertView == null) {
        convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
        holder = new MyHolder();
        holder.picIv = (ImageView) convertView.findViewById(R.id.pic_iv);
        convertView.setTag(holder);
      } else {
        holder = (MyHolder) convertView.getTag();
      }

      Glide.with(parent.getContext())
          .load(mData.get(position))
          .dontAnimate()
          .into(holder.picIv);

      return convertView;
    }
  }

  static class MyHolder {
    public ImageView picIv;
  }
}