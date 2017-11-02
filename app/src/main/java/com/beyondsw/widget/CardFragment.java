package com.beyondsw.widget;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.beyondsw.lib.widget.StackCardsView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wensefu on 17-3-4.
 */
public class CardFragment extends Fragment implements Handler.Callback,StackCardsView.OnCardSwipedListener
        ,View.OnClickListener,CompoundButton.OnCheckedChangeListener{

    private static final String TAG ="StackCardsView-DEMO";

    private StackCardsView mCardsView;
    private CardAdapter mAdapter;
    private HandlerThread mWorkThread;
    private Handler mWorkHandler;
    private Handler mMainHandler;
    private static final int MSG_START_LOAD_DATA = 1;
    private static final int MSG_DATA_LOAD_DONE = 2;
    private volatile int mStartIndex;
    private static final int PAGE_COUNT = 10;

    private View mLeftBtn;
    private View mRightBtn;
    private View mUpBtn;
    private View mDownBtn;
    private CheckBox mCb;

    private Callback mCallback;

    public interface Callback {
        void onViewPagerCbChanged(boolean checked);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.page1,null);

        mCb = Utils.findViewById(root, R.id.view_pager_cb);
        mCb.setOnCheckedChangeListener(this);
        if (mCallback != null) {
            mCallback.onViewPagerCbChanged(mCb.isChecked());
        }
        mLeftBtn = Utils.findViewById(root,R.id.left);
        mRightBtn = Utils.findViewById(root,R.id.right);
        mUpBtn = Utils.findViewById(root,R.id.up);
        mDownBtn = Utils.findViewById(root, R.id.down);
        mLeftBtn.setOnClickListener(this);
        mRightBtn.setOnClickListener(this);
        mUpBtn.setOnClickListener(this);
        mDownBtn.setOnClickListener(this);

        mCardsView = Utils.findViewById(root,R.id.cards);
        mCardsView.addOnCardSwipedListener(this);
        mAdapter = new CardAdapter();
        mCardsView.setAdapter(mAdapter);
        mMainHandler = new Handler(this);
        mWorkThread = new HandlerThread("data_loader");
        mWorkThread.start();
        mWorkHandler = new Handler(mWorkThread.getLooper(),this);
        mWorkHandler.obtainMessage(MSG_START_LOAD_DATA).sendToTarget();
        return root;
    }

    void setCallback(Callback callback) {
        mCallback = callback;
        if (mCb != null) {
            mCallback.onViewPagerCbChanged(mCb.isChecked());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCardsView.removeOnCardSwipedListener(this);
        mWorkThread.quit();
        mWorkHandler.removeMessages(MSG_START_LOAD_DATA);
        mMainHandler.removeMessages(MSG_DATA_LOAD_DONE);
        mStartIndex = 0;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mCallback != null) {
            mCallback.onViewPagerCbChanged(isChecked);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mLeftBtn) {
            mCardsView.removeCover(StackCardsView.SWIPE_LEFT);
        } else if (v == mRightBtn) {
            mCardsView.removeCover(StackCardsView.SWIPE_RIGHT);
        } else if (v == mUpBtn) {
            mCardsView.removeCover(StackCardsView.SWIPE_UP);
        } else if (v == mDownBtn) {
            mCardsView.removeCover(StackCardsView.SWIPE_DOWN);
        }
    }

    @Override
    public void onCardDismiss(int direction) {
        mAdapter.remove(0);
        if (mAdapter.getCount() < 3) {
            if (!mWorkHandler.hasMessages(MSG_START_LOAD_DATA)) {
                mWorkHandler.obtainMessage(MSG_START_LOAD_DATA).sendToTarget();
            }
        }
    }

    @Override
    public void onCardScrolled(View view, float progress, int direction) {
        Log.d(TAG, "onCardScrolled: view=" + view.hashCode() + ", progress=" + progress + ",direction=" + direction);
        Object tag = view.getTag();
        if (tag instanceof ImageCardItem.ViewHolder) {
            ImageCardItem.ViewHolder vh = (ImageCardItem.ViewHolder)tag;
            if (progress > 0) {
                switch (direction){
                    case StackCardsView.SWIPE_LEFT:
                        vh.left.setAlpha(progress);
                        vh.right.setAlpha(0f);
                        vh.up.setAlpha(0f);
                        vh.down.setAlpha(0f);
                        break;
                    case StackCardsView.SWIPE_RIGHT:
                        vh.right.setAlpha(progress);
                        vh.left.setAlpha(0f);
                        vh.up.setAlpha(0f);
                        vh.down.setAlpha(0f);
                        break;
                    case StackCardsView.SWIPE_UP:
                        vh.up.setAlpha(progress);
                        vh.left.setAlpha(0f);
                        vh.right.setAlpha(0f);
                        vh.down.setAlpha(0f);
                        break;
                    case StackCardsView.SWIPE_DOWN:
                        vh.down.setAlpha(progress);
                        vh.left.setAlpha(0f);
                        vh.right.setAlpha(0f);
                        vh.up.setAlpha(0f);
                        break;
                }
            } else {
                vh.left.setAlpha(0f);
                vh.right.setAlpha(0f);
                vh.up.setAlpha(0f);
                vh.down.setAlpha(0f);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){
            case MSG_START_LOAD_DATA:{
                List<BaseCardItem> data = loadData(mStartIndex);
                mMainHandler.obtainMessage(MSG_DATA_LOAD_DONE,data).sendToTarget();
                break;
            }
            case MSG_DATA_LOAD_DONE:{
                List<BaseCardItem> data = (List<BaseCardItem>) msg.obj;
                mAdapter.appendItems(data);
                mStartIndex += sizeOfImage(data);
                break;
            }
        }
        return true;
    }

    private int sizeOfImage(List<BaseCardItem> items){
        if(items==null){
            return 0;
        }
        int size = 0;
        for (BaseCardItem item : items) {
            if (item instanceof ImageCardItem) {
                size++;
            }
        }
        return size;
    }


    private List<BaseCardItem> loadData(int startIndex) {
        if (startIndex < ImageUrls.images.length) {
            final int endIndex = Math.min(mStartIndex + PAGE_COUNT, ImageUrls.images.length - 1);
            List<BaseCardItem> result = new ArrayList<>(endIndex - startIndex + 1);
            for (int i = startIndex; i <= endIndex; i++) {
                ImageCardItem item = new ImageCardItem(getActivity(), ImageUrls.images[i], ImageUrls.labels[i]);
                item.dismissDir = StackCardsView.SWIPE_ALL;
                item.fastDismissAllowed = true;
                result.add(item);
            }
            if (startIndex == 0) {
                ScrollCardItem item = new ScrollCardItem(getActivity());
                result.add(result.size() / 2, item);
            }
            return result;
        }
        return null;
    }
}
