
package com.baidu.download;

import com.baidu.download.engine.DownloadManager;
import com.baidu.download.entity.Download;
import com.baidu.download.entity.Download.DownloadListener;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class ListActivity extends Activity {
    public static final String TAG = "ListActivity";
    private ListView lv;
    private List<Download> mDownloadList;
    private List<Download> mDownloadOverList;
    private DownloadAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        findView();
        initView();
    }

    private void findView() {
        lv = (ListView) findViewById(R.id.lv);
        mDownloadList = DownloadManager.getInstance().getDownloads();
        mDownloadOverList = DownloadManager.getInstance().getDownloadOverList();
    }

    private void initView() {
        adapter = new DownloadAdapter();
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e("@@@", "onClick");
            }
        });
    }

    private class DownloadAdapter extends BaseAdapter {

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Download download;

            if (position < mDownloadList.size()) {
                download = mDownloadList.get(position);
            } else {
                download = mDownloadOverList.get(position - mDownloadList.size());
            }

            View view = View.inflate(ListActivity.this, R.layout.item_lv, null);
            final TextView tv = (TextView) view.findViewById(R.id.tv_title);
            final ProgressBar pb = (ProgressBar) view.findViewById(R.id.pb);
            final Button button = (Button) view.findViewById(R.id.bt);

            Log.e(TAG,
                    "progress::" + download.getDownloadLength() + "::total::"
                            + download.getTotalLenght());
           
            pb.setMax((int) download.getTotalLenght());
            pb.setProgress((int) download.getDownloadLength());
            button.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if ("暂停".equals(button.getText().toString().trim())) {
                        button.setText("继续");
                        DownloadManager.getInstance().pause(download);
                    } else {
                        button.setText("暂停");
                        DownloadManager.getInstance().start(download);
                    }
                }
            });

            if (download.isDownloadOver()) {
                tv.setText("下载成功");
            } else if (download.isWait()) {
                tv.setText("等待中");
            } else if (download.getDownloadState() == Download.STATE_PAUSE) {
                tv.setText("暂停。。。");
            } else {
                tv.setText("正在下载。。。   ");
            }
            download.setDownloadListener(new DownloadListener() {

                @Override
                public void onTotalLength(long total) {
                    Log.e("@@@", "onTotalLength:::" + total);
                    pb.setMax((int) total);
                }

                @Override
                public void onSuccess(File file) {
                    Log.e("@@@", "onSuccess");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onProgress(long progress, long total) {
                    Log.e("@@@", "onProgress:::" + progress);

                    if (progress == 0 || total == 0) {
                        Log.e(TAG, "000000000:::progress::" + progress + "total::" + total);
                    }

                    pb.setProgress((int) progress);

                    if (pb.getMax() != total) {
                        pb.setMax((int) total);
                    }

                }

                @Override
                public void onFailed(final int errorCode) {
                    Log.e("@@@", "onFailed");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tv.setText("失败:" + errorCode);
                        }
                    });
                }

                @Override
                public void onStatusChange() {
                    if (download.getDownloadState() == Download.STATE_PAUSE) {
                        tv.setText("暂停");
                    } else if (download.getDownloadState() == Download.STATE_WAIT) {
                        tv.setText("等待");
                    }
                }
            });

            return view;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public int getCount() {
            return mDownloadList.size() + mDownloadOverList.size();
        }
    }
}
