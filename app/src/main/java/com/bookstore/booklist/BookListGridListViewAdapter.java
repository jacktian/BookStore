package com.bookstore.booklist;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bookstore.bookparser.BookCategory;
import com.bookstore.main.R;
import com.bookstore.provider.DB_Column;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Administrator on 2016/3/1.
 */
public class BookListGridListViewAdapter extends BaseAdapter {
    public static final String TAG = "BookStore";
    public Context mContext;
    BookCategory bookCategory;
    private TypedArray mColor_list;
    private HashMap<Integer, Cursor> dataMap = null;

    public BookListGridListViewAdapter(Context context) {
        mContext = context;
        mColor_list = mContext.getResources().obtainTypedArray(R.array.color_list);
        bookCategory = new BookCategory();
        dataMap = new HashMap<Integer, Cursor>();
    }

    @Override

    public int getCount() {
        return bookCategory.getCategoryCount();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View listItemView;
        if (convertView == null) {
            LayoutInflater mInflater = LayoutInflater.from(mContext);
            listItemView = mInflater.inflate(R.layout.booklist_gridview_list_item, null);
            View morebooks = listItemView.findViewById(R.id.MoreBooks);
            morebooks.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    // TODO Auto-generated method stub
                    Log.i("csf", "btn click");
                }

            });

            View color_panel = listItemView.findViewById(R.id.color_view);
            color_panel.setBackgroundColor(getColor(position));
        } else {
            listItemView = convertView;
        }
        bindView(listItemView, position);
        return listItemView;
    }

    public void bindView(View listItemView, int position) {
        listItemView.setVisibility(View.VISIBLE);
        TextView category_name = (TextView) listItemView.findViewById(R.id.category_name);
        category_name.setText(bookCategory.getCategoryName(position));

        ArrayList<BookCategory.CategoryItem> list = bookCategory.getUser_category_list();
        int key = list.get(position).category_code;
        Cursor dataCursor = dataMap.get(key);
        if (dataCursor == null) {
            listItemView.setVisibility(View.INVISIBLE);
            return;
        }

        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();

        //show cover1
        if (dataCursor.moveToPosition(position)) {
            //String cover1Url = mDataCursor.getString(Projection.BookInfo.COLUMN_IMG_LARGE);
            String cover1Url = dataCursor.getString(dataCursor.getColumnIndex(DB_Column.BookInfo.IMG_LARGE));
            ImageView cover1 = (ImageView) listItemView.findViewById(R.id.cover1);
            ImageLoader.getInstance().displayImage(cover1Url, cover1, options);
        }
        //show cover2
        if (dataCursor.moveToPosition(position + 1)) {
            //String cover2Url = mDataCursor.getString(Projection.BookInfo.COLUMN_IMG_LARGE);
            String cover2Url = dataCursor.getString(dataCursor.getColumnIndex(DB_Column.BookInfo.IMG_LARGE));
            ImageView cover2 = (ImageView) listItemView.findViewById(R.id.cover2);
            ImageLoader.getInstance().displayImage(cover2Url, cover2, options);
        }
        //show cover3
        if (dataCursor.moveToPosition(position + 2)) {
            //String cover3Url = mDataCursor.getString(Projection.BookInfo.COLUMN_IMG_LARGE);
            String cover3Url = dataCursor.getString(dataCursor.getColumnIndex(DB_Column.BookInfo.IMG_LARGE));
            ImageView cover3 = (ImageView) listItemView.findViewById(R.id.cover3);
            ImageLoader.getInstance().displayImage(cover3Url, cover3, options);
        }
    }

    public int getColor(int position) {
        return mColor_list.getColor(position % mColor_list.length(), 0);
    }

    public void registerDataCursor(int category_code, Cursor dataCursor) {
        if (dataCursor == null) {
            return;
        }
        dataMap.put(category_code, dataCursor);
    }

    public void clearDataCursor() {
        dataMap.clear();
    }
}
