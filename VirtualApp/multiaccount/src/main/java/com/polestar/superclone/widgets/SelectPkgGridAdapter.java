package com.polestar.superclone.widgets;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.polestar.clone.BitmapUtils;
import com.polestar.superclone.R;

import java.util.List;

/**
 * Created by PolestarApp on 2017/7/17.
 */

public class SelectPkgGridAdapter extends BaseAdapter{

    private List<SelectGridAppItem> appInfos;
    private Context mContext;

    public SelectPkgGridAdapter(Context context, List<SelectGridAppItem> list) {
        super();
        mContext = context;
        appInfos = list;
    }

    @Override
    public int getCount() {
        return appInfos.size();
    }

    @Override
    public Object getItem(int position) {
        if ( appInfos == null) {
            return  null;
        }
        if (position < appInfos.size() && position >= 0) {
            return  appInfos.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = new SelectAppCell(mContext);
        ImageView appIcon = (ImageView) view.findViewById(R.id.app_icon);
        ImageView cb = (ImageView) view.findViewById(R.id.select_cb_img);
        TextView appName = (TextView) view.findViewById(R.id.app_name);

        SelectGridAppItem appModel = (SelectGridAppItem) getItem(i);
        if (appModel != null) {
//            int userId = CloneManager.getInstance(mContext).getNextAvailableUserId(appModel.pkg);
            int userId = 0;
            if (userId > 0) {
                appIcon.setImageBitmap(BitmapUtils.createBadgeIcon(mContext, appModel.icon, userId));
            } else {
                appIcon.setImageDrawable(appModel.icon);
            }
            appName.setText(appModel.name);
            if (appModel.selected ) {
                cb.setImageResource(R.drawable.selectd);
            } else{
                cb.setImageResource(R.drawable.not_select);
            }
        }
        view.setTag(appModel);
        return view;
    }
}
