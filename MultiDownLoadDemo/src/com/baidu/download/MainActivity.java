
package com.baidu.download;

import com.baidu.download.engine.DownloadManager;
import com.baidu.download.entity.Novel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String url = "http://appgame.3g.ifeng.com/gamestore/201402/ifengvideo6.6.1_Android_2.1_V6.6.1_10001.apk";

    private ListView lv;

    private Button bt;

    private List<Novel> mList;

    private DownloadListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        lv = (ListView) findViewById(R.id.lv);
        bt = (Button) findViewById(R.id.bt);

        bt.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ListActivity.class));
            }
        });

        mList = new ArrayList<Novel>();
        DownloadManager.getInstance().onCreate(MainActivity.this);
        DownloadManager.getInstance().setNotificationVisibility(true);

        for (int i = 0; i < 10; i++) {
            Novel novel = new Novel();
            novel.setId(i + 1);
            novel.setUrl(url);
            mList.add(novel);
        }

        adapter = new DownloadListAdapter();
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, "第" + position + "个已经添加到下载队列", 0).show();
                    }
                });
                DownloadManager.getInstance().enqueue(MainActivity.this,
                        (Novel) adapter.getItem(position));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DownloadManager.getInstance().onDestroy(MainActivity.this);
    }

    private class DownloadListAdapter extends BaseAdapter {

        @Override
        public int getCount() {

            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = new TextView(MainActivity.this);
            view.setText(position + "");
            view.setTextSize(26);
            return view;
        }
    }
}
