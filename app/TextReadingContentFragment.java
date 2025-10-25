package net.convocatis.convocatis.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.convocatis.convocatis.R;
import net.convocatis.convocatis.database.model.TextModel;

/**
 * Created by reactor on 1/25/16.
 */
public class TextReadingContentFragment extends BaseFragment {

    public static final String MAIN_TEXT_PARAM = "MAIN_TEXT_PARAM";
    public static final String SUB_TEXT_PARAM = "SUB_TEXT_PARAM";

    public String mParamMainText, mParamSubText;

    private TextView mMainText, mSubText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mParamMainText = getArguments().getString(MAIN_TEXT_PARAM);
        mParamSubText = getArguments().getString(SUB_TEXT_PARAM);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.text_reading_content_fragment, container, false);

        mMainText = (TextView) v.findViewById(R.id.main_text);
        mSubText = (TextView) v.findViewById(R.id.sub_text);

//        if (mParamMainText != null) {
//            mMainText.setText(mParamMainText);
//            mMainText.setVisibility(View.VISIBLE);
//        } else {
//            mMainText.setVisibility(View.GONE);
//        }

        if (mParamSubText != null) {
            mSubText.setText(mParamSubText);
            mSubText.setVisibility(View.VISIBLE);
        } else {
            mSubText.setVisibility(View.GONE);
        }

        return v;
    }
}
