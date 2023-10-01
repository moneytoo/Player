package com.brouken.player.encrypt;


import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.core.util.ObjectsCompat;

import org.json.JSONException;
import org.json.JSONObject;

public class NyVideo implements Parcelable {
    public String name = "";
    public String path;
    public String passWord = null;
    public int download = 0;//0-
    public int cost = 0;
    public Info info;
    public boolean isChecked;
    public boolean httpRedirect = true;

    public NyVideo() {

    }

    public NyVideo(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public NyVideo(String name, String path, String passWord) {
        this.name = name;
        this.path = path;
        this.passWord = passWord;
        this.httpRedirect = true;
    }

    public NyVideo(String name, String path, String passWord, boolean httpRedirect) {
        this.name = name;
        this.path = path;
        this.passWord = passWord;
        this.httpRedirect = httpRedirect;
    }


    public NyVideo(String name, String path, String passWord, int download, Info Info, int cost) {
        this.name = name;
        this.path = path;
        this.passWord = passWord;
        this.download = download;
        this.cost = cost;
        this.info = Info;
    }

    public NyVideo(String name, String path, String passWord, int download, Info Info) {
        this.name = name;
        this.path = path;
        this.passWord = passWord;
        this.cost = 0;
        this.download = download;
        this.info = Info;
    }

    public NyVideo(Cursor cursor) {
        name = cursor.getString(0);
        path = cursor.getString(1);
        passWord = cursor.getString(2);
        download = cursor.getInt(3);
        info.id = cursor.getLong(4);
        info.size = cursor.getLong(5);
        info.duration = cursor.getInt(6);
        info.width = cursor.getInt(7);
        info.height = cursor.getInt(8);
        info.progress = cursor.getInt(9);
        info.thumbUrl = cursor.getString(10);
        cost = cursor.getInt(11);
        isChecked = false;
    }

    protected NyVideo(Parcel in) {
        name = in.readString();
        path = in.readString();
        passWord = in.readString();
        download = in.readInt();
        cost = in.readInt();
     /*   Info.id = in.readLong();
        Info.size = in.readLong();
        Info.duration = in.readInt();
        Info.width = in.readInt();
        Info.height = in.readInt();
        Info.progress = in.readInt();
        Info.thumbUrl = in.readString();*/
        isChecked = false;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        //sequence must be consistent with previous method: NyVideo(parcel in)
        dest.writeString(name);
        dest.writeString(path);
        dest.writeString(passWord);
        dest.writeInt(download);
        dest.writeInt(cost);
    /*    dest.writeLong(Info.id);
        dest.writeLong(Info.size);
        dest.writeInt(Info.duration);
        dest.writeInt(Info.width);
        dest.writeInt(Info.height);
        dest.writeInt(Info.progress);
        dest.writeString(Info.thumbUrl);*/
    }

    public JSONObject toJSONObject() {
        JSONObject object = new JSONObject();
        try {
            object.put("name", getName());
            object.put("path", getPath());
            object.put("passWord", getPassWord());
            object.put("download", getDownload());
            object.put("unlock", getCost());
            // object.put("thumbUrl", Info.thumbUrl);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<NyVideo> CREATOR = new Creator<NyVideo>() {
        @Override
        public NyVideo createFromParcel(Parcel in) {
            return new NyVideo(in);
        }

        @Override
        public NyVideo[] newArray(int size) {
            return new NyVideo[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public int getDownload() {
        return download;
    }

    public void setDownload(int download) {
        this.download = download;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    /*
    @Override
    public boolean equals(Object o) {
        if (ObjectsCompat.equals(this, o)) return true;
        if (o == null) return false;
        NyVideo nyVideo = (NyVideo) o;
        return ObjectsCompat.equals(name, nyVideo.name) &&
                ObjectsCompat.equals(path, nyVideo.path);
    }*/

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(name, path);
    }

    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        if (obj instanceof NyVideo) {
            NyVideo temp = (NyVideo) obj;
            //   if(this.name == temp.name && this.path== temp.path) return true;
            return this.path.equals(temp.path);

        }
        return false;

    }

 /*   @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return (this.name.hashCode() + this.path.hashCode());
        // return this.path.hashCode();
    }
*/
}
