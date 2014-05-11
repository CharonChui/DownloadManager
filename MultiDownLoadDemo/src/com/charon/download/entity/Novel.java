
package com.charon.download.entity;

import com.charon.download.dao.inter.IDownloader;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * 小说
 * 
 * @author xuchuanren
 */
public class Novel implements Serializable, Parcelable, IDownloader {

    private static final long serialVersionUID = -2926600819222885788L;
    private String title;
    private String des;
    private String imageUrl;
    private String url;
    private int id;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getID() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(des);
        dest.writeString(imageUrl);
        dest.writeString(url);
    }

    public static final Parcelable.Creator<Novel> CREATOR = new Parcelable.Creator<Novel>() {
        public Novel createFromParcel(Parcel in) {
            return new Novel(in);
        }

        public Novel[] newArray(int size) {
            return new Novel[size];
        }
    };

    private Novel(Parcel in) {
        id = in.readInt();
        title = in.readString();
        des = in.readString();
        imageUrl = in.readString();
        url = in.readString();
    }

    public Novel() {

    }

}
