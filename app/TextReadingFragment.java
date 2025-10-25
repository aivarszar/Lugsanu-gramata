package net.convocatis.convocatis.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import net.convocatis.convocatis.R;
import net.convocatis.convocatis.database.model.NotificationModel;
import net.convocatis.convocatis.database.model.TextModel;
import net.convocatis.convocatis.database.model.TextUsageModel;
import net.convocatis.convocatis.diskmanager.DiskTask;
import net.convocatis.convocatis.networking.SynchronizationService;
import net.convocatis.convocatis.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

/**
 * Created by reactor on 1/25/16.
 */
public class TextReadingFragment extends BaseFragment implements MainActivity.OnOptionsItemSelectedFragmentListener {

    public static final String TEXT_MODEL_PARAM = "TEXT_MODEL_PARAM";

    public TextModel mTextModel;
    public ViewPager mViewPager;
    public TextContentAdapter mAdapter;
    public ArrayList<HashMap<String, String>> mData;
    public View mPanel1, mPanel2, mMainPanel, mSubPanel;
    public TextView mMainProgress, mSubProgress;
    public View mNextMainButton, mPrevMainButton, mNextSubButton, mPrevSubButton;
    public ProgressBar mSubProgressBar, mMainProgressBar;
    public TextView mMainText;
    public View mSeparator;
    public ScrollView mMainTextScroll;

    @Override
    public void onOptionsItemSelectedFragment(MenuItem item) {
        if (mPanel1.getVisibility() == View.GONE) {
            mPanel1.setVisibility(View.VISIBLE);
            mPanel2.setVisibility(View.VISIBLE);
        } else {
            mPanel1.setVisibility(View.GONE);
            mPanel2.setVisibility(View.GONE);
        }
    }

    public class TextContentAdapter extends FragmentPagerAdapter {

        public TextContentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            HashMap<String, String> texts = mData.get(position);

            TextReadingContentFragment fragment = new TextReadingContentFragment();

            Bundle arguments = new Bundle();
            arguments.putSerializable(TextReadingContentFragment.MAIN_TEXT_PARAM, texts.get("main"));
            arguments.putSerializable(TextReadingContentFragment.SUB_TEXT_PARAM, texts.get("sub"));

            fragment.setArguments(arguments);

            return fragment;
        }

        @Override
        public int getCount() {
            return mData.size();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTextModel = (TextModel) getArguments().getSerializable(TEXT_MODEL_PARAM);

        mData = new ArrayList<>();
        parseText();


    }

    public void addToData(String mainText, String subText, String mainTextProgress, String mainTextCount, String subTextProgress, String subTextCount,
                          String secondaryProgress, String secondaryCount) {
        HashMap<String, String> abc = new HashMap<>();

        abc.put("main", mainText);
        abc.put("mainPro", mainTextProgress);
        abc.put("mainCo", mainTextCount);

        abc.put("sub", subText);
        abc.put("subPro", subTextProgress);
        abc.put("subCo", subTextCount);

        abc.put("secPro", secondaryProgress);
        abc.put("secCo", secondaryCount);

        mData.add(abc);
    }

    public void parseText() {

        mData.clear();

        String text = mTextModel.text;

        String currentMainText = null;
        int currentRepetitionCount = 1;
        ArrayList<String> currentSubtexts = new ArrayList<String>();

        int currentIndex = 0;
        int subtextIndex = -1;
        int maintextIndex = -1;

        while (true) {
            maintextIndex = text.indexOf(">>", currentIndex);
            subtextIndex = text.indexOf("|", currentIndex);

            if (maintextIndex == -1 && subtextIndex == -1) {
                break;
            }

            if (maintextIndex != -1 && (subtextIndex == -1 || subtextIndex > maintextIndex)) {
                //found main text

                if (currentMainText != null || currentSubtexts.size() > 0) {
                    if (currentSubtexts.size() == 0) {
                        for (int i = 0; i < currentRepetitionCount; i++) {
                            addToData(currentMainText, null, "" + i, "" + currentRepetitionCount, null, null, "" + i, "" + currentRepetitionCount);
                        }
                    } else {

                        if (currentMainText == null) {
                            currentRepetitionCount = 1;
                        }

                        for (int c = 0; c < currentRepetitionCount; c++) {

                            int subRepetition = 0;
                            int subCount = 0;
                            String subText = null;

                            for (int i = 0; i < currentSubtexts.size(); i++) {

                                if (subText == null || !subText.equals(currentSubtexts.get(i))) {
                                    subRepetition = 0;
                                    subText = currentSubtexts.get(i);

                                    subCount = 0;
                                    for (int j = i; j < currentSubtexts.size(); j++) {
                                        if (subText.equals(currentSubtexts.get(j))) {
                                            subCount++;
                                        } else {
                                            break;
                                        }
                                    }

                                } else {
                                    subRepetition++;
                                }

                                addToData(currentMainText, currentSubtexts.get(i), "" + c, "" + currentRepetitionCount, "" + subRepetition, "" + subCount, "" + (c * currentSubtexts.size() + i), "" + (currentRepetitionCount * currentSubtexts.size()));
                            }
                        }
                        currentSubtexts.clear();
                    }
                }

                int endMaintextIndex = text.indexOf("<<", maintextIndex);

                if (endMaintextIndex == -1) {
                    endMaintextIndex = text.length();
                }
                currentIndex = endMaintextIndex + 2;

                currentMainText = text.substring(maintextIndex + 2, endMaintextIndex);

                int repetitionSignIndex = currentMainText.indexOf("^");
                currentRepetitionCount = 1;

                if (repetitionSignIndex > -1) {
                    String repetitionsCountString = currentMainText.substring(0, repetitionSignIndex);

                    try {
                        currentRepetitionCount = Integer.parseInt(repetitionsCountString);
                        if (currentRepetitionCount > 1000) {
                            currentRepetitionCount = 1000;
                        }
                        if (currentRepetitionCount < 1) {
                            currentRepetitionCount = 1;
                        }

                        currentMainText = currentMainText.substring(repetitionSignIndex + 1);

                    } catch (NumberFormatException nfe) {
                    }
                }

            } else {
                //found sub text
                int endsubtextIndex1 = text.indexOf(">>", subtextIndex + 1);
                int endsubtextIndex2 = text.indexOf("|", subtextIndex + 1);
                int endsubtextIndex = 0;

                if (endsubtextIndex1 == -1) {
                    endsubtextIndex1 = Integer.MAX_VALUE;
                }
                if (endsubtextIndex2 == -1) {
                    endsubtextIndex2 = Integer.MAX_VALUE;
                }

                endsubtextIndex = Math.min(endsubtextIndex1, endsubtextIndex2);

                if (endsubtextIndex > text.length()) {
                    endsubtextIndex = text.length();
                }

                currentIndex = endsubtextIndex;

                String subText = text.substring(subtextIndex + 1, endsubtextIndex);

                int repetitionSignIndex = subText.indexOf("^");
                int repetitionCount = 1;

                if (repetitionSignIndex > -1) {
                    String repetitionsCountString = subText.substring(0, repetitionSignIndex);

                    try {
                        repetitionCount = Integer.parseInt(repetitionsCountString);
                        if (repetitionCount > 1000) {
                            repetitionCount = 1000;
                        }
                        if (repetitionCount < 1) {
                            repetitionCount = 1;
                        }

                        subText = subText.substring(repetitionSignIndex + 1);

                    } catch (NumberFormatException nfe) {
                    }
                }

                for (int i = 0; i < repetitionCount; i++) {
                    currentSubtexts.add(subText);
                }
            }

            if (currentIndex >= text.length()) {
                break;
            }

        }

        if (currentMainText != null || currentSubtexts.size() > 0) {
            if (currentSubtexts.size() == 0) {
                for (int i = 0; i < currentRepetitionCount; i++) {
                    addToData(currentMainText, null, "" + i, "" + currentRepetitionCount, null, null, "" + i, "" + currentRepetitionCount);
                }
            } else {

                if (currentMainText == null) {
                    currentRepetitionCount = 1;
                }

                for (int c = 0; c < currentRepetitionCount; c++) {

                    int subRepetition = 0;
                    int subCount = 0;
                    String subText = null;

                    for (int i = 0; i < currentSubtexts.size(); i++) {

                        if (subText == null || !subText.equals(currentSubtexts.get(i))) {
                            subRepetition = 0;
                            subText = currentSubtexts.get(i);

                            subCount = 0;
                            for (int j = i; j < currentSubtexts.size(); j++) {
                                if (subText.equals(currentSubtexts.get(j))) {
                                    subCount++;
                                } else {
                                    break;
                                }
                            }

                        } else {
                            subRepetition++;
                        }

                        addToData(currentMainText, currentSubtexts.get(i), "" + c, "" + currentRepetitionCount, "" + subRepetition, "" + subCount, "" + (c * currentSubtexts.size() + i), "" + (currentRepetitionCount * currentSubtexts.size()));
                    }
                }
                currentSubtexts.clear();
            }
        }

        if (mData.size() == 0) {
            addToData(null, text, "0", "1", "0", "1", "0", "1");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.text_reading_fragment, container, false);

        mMainText = (TextView) v.findViewById(R.id.main_text);
        mMainTextScroll = (ScrollView) v.findViewById(R.id.main_text_scroll);

        mSeparator = v.findViewById(R.id.separator);

        if (mData.size() == 0) {
            v.post(new Runnable() {
                @Override
                public void run() {
                    mMainActivity.goBack();
                }
            });

            return v;
        }

        mNextMainButton = v.findViewById(R.id.next_main_button);
        mPrevMainButton = v.findViewById(R.id.prev_main_button);
        mNextSubButton = v.findViewById(R.id.next_sub_button);
        mPrevSubButton = v.findViewById(R.id.prev_sub_button);

        mSubProgressBar = (ProgressBar) v.findViewById(R.id.sub_progress_bar);
        mMainProgressBar = (ProgressBar) v.findViewById(R.id.main_progress_bar);

        mNextMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String mainText = mData.get(mViewPager.getCurrentItem()).get("main");
                String progress = mData.get(mViewPager.getCurrentItem()).get("mainPro");

                for (int i = mViewPager.getCurrentItem(); i < mData.size(); i++) {
                    if (

                            (!TextUtils.equals(mData.get(i).get("main"), mainText) ||
                                    !TextUtils.equals(progress, mData.get(i).get("mainPro")))

                                    && !TextUtils.isEmpty(mData.get(i).get("main"))) {
                        mViewPager.setCurrentItem(i);
                        break;
                    }
                }

            }
        });

        mPrevMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mainText = mData.get(mViewPager.getCurrentItem()).get("main");
                String progress = mData.get(mViewPager.getCurrentItem()).get("mainPro");

                for (int i = mViewPager.getCurrentItem(); i >= 0; i--) {
                    if (
                            (!TextUtils.equals(mData.get(i).get("main"), mainText) ||
                                    !TextUtils.equals(progress, mData.get(i).get("mainPro")))

                                    && !TextUtils.isEmpty(mData.get(i).get("main"))) {


//                        int selectedItem = i;
//                        for (int j = i; j >= 0; j--) {
//                            if (!TextUtils.equals(mData.get(i).get("main"), mData.get(j).get("main"))) {
//                                break;
//                            }
//
//                            selectedItem = j;
//                        }

                        //mViewPager.setCurrentItem(selectedItem);
                        mViewPager.setCurrentItem(i);

                        break;
                    }
                }
            }
        });

        mNextSubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int currentItem = mViewPager.getCurrentItem();

                if (currentItem < mData.size() - 1) {
                    mViewPager.setCurrentItem(currentItem + 1);
                }

//                String subText = mData.get(mViewPager.getCurrentItem()).get("sub");
//
//                for (int i = mViewPager.getCurrentItem(); i < mData.size(); i++) {
//                    if (!TextUtils.equals(mData.get(i).get("sub"), subText) && !TextUtils.isEmpty(mData.get(i).get("sub"))) {
//                        mViewPager.setCurrentItem(i);
//                        break;
//                    }
//                }
            }
        });

        mPrevSubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int currentItem = mViewPager.getCurrentItem();


                if (currentItem > 0) {
                    mViewPager.setCurrentItem(currentItem - 1);
                }

//                String subText = mData.get(mViewPager.getCurrentItem()).get("sub");
//
//                for (int i = mViewPager.getCurrentItem(); i >= 0; i--) {
//                    if (!TextUtils.equals(mData.get(i).get("sub"), subText) && !TextUtils.isEmpty(mData.get(i).get("sub"))) {
//                        int selectedItem = i;
//                        for (int j = i; j >= 0; j--) {
//                            if (!TextUtils.equals(mData.get(i).get("sub"), mData.get(j).get("sub"))) {
//                                break;
//                            }
//
//                            selectedItem = j;
//                        }
//
//                        mViewPager.setCurrentItem(selectedItem);
//                        break;
//                    }
//                }
            }
        });

        mViewPager = (ViewPager) v.findViewById(R.id.pager);
        mAdapter = new TextContentAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mAdapter);
        mPanel1 = v.findViewById(R.id.panel1);
        mPanel2 = v.findViewById(R.id.panel2);
        mMainPanel = v.findViewById(R.id.main_panel);
        mSubPanel = v.findViewById(R.id.sub_panel);
        mMainProgress = (TextView) v.findViewById(R.id.main_progress);
        mSubProgress = (TextView) v.findViewById(R.id.sub_progress);

        mMainActivity.setOnOptionsItemSelectedListener(this);

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                updateUI(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        updateUI(0);


        if (mTextModel.backendId != null) {

            new DiskTask() {

                @Override
                public void getData() {

                    List<NotificationModel> unhidden = NotificationModel.getUnhiddenNotifications();

                    TextUsageModel model = new TextUsageModel();

                    model.notificationIds = "";
                    model.textId = mTextModel.backendId;

                    for (NotificationModel m : unhidden) {
                        if (m.backendId != null) {
                            if (model.notificationIds.length() > 0) {
                                model.notificationIds += ",";
                            }

                            model.notificationIds += m.backendId;
                        }
                    }

                    model.start = System.currentTimeMillis() / 1000;
                    model.persist();
                }

                @Override
                public void onDataReceived() {
                    SynchronizationService.startSync(true);
                }
            }.execute(TextReadingFragment.this);

        }

        if (mData.size() > 1) {
            mPanel1.setVisibility(View.VISIBLE);
            mPanel2.setVisibility(View.VISIBLE);
        }

        return v;
    }

    private void updateUI(int position) {
        HashMap<String, String> data = mData.get(position);

        String main = data.get("main");
        String mainPro = data.get("mainPro");
        String mainCo = data.get("mainCo");

        if (main == null) {
            mMainPanel.setVisibility(View.GONE);
        } else {
            mMainPanel.setVisibility(View.VISIBLE);

            int mainProInt = Integer.parseInt(mainPro);
            int mainCoInt = Integer.parseInt(mainCo);
            mMainProgress.setText("Main text repetition: " + (mainProInt + 1) + "/" + (mainCoInt));

            mMainProgressBar.setMax(mData.size() - 1);
            mMainProgressBar.setProgress(position);
        }

        String sub = data.get("sub");
        String subPro = data.get("subPro");
        String subCo = data.get("subCo");

        String secPro = data.get("secPro");
        String secCo = data.get("secCo");

        if (sub == null) {
            mSubPanel.setVisibility(View.GONE);
        } else {
            mSubPanel.setVisibility(View.VISIBLE);

            int subProInt = Integer.parseInt(subPro);
            int subCoInt = Integer.parseInt(subCo);

            int secProInt = Integer.parseInt(secPro);
            int secCoInt = Integer.parseInt(secCo);

            mSubProgress.setText("Sub text repetition: " + (subProInt + 1) + "/" + (subCoInt));

            mSubProgressBar.setMax(secCoInt - 1);
            mSubProgressBar.setProgress(secProInt);
        }

        mSeparator.setVisibility(View.VISIBLE);
        mViewPager.setVisibility(View.VISIBLE);
        mMainTextScroll.setVisibility(View.VISIBLE);

        String text = mData.get(position).get("main");
        String subText = mData.get(position).get("sub");

        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(subText)) {
            mSeparator.setVisibility(View.GONE);
        }


        if (TextUtils.isEmpty(text)) {
            mMainTextScroll.setVisibility(View.GONE);
        } else if (TextUtils.isEmpty(subText)) {
            mViewPager.setVisibility(View.GONE);
        }

        if (text == null) {
            text = "";
        }

        mMainText.setText(text);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mMainActivity.setOnOptionsItemSelectedListener(null);


        if (mTextModel.backendId != null) {

            new DiskTask() {

                @Override
                public void getData() {

                    TextUsageModel model = new TextUsageModel();

                    model.textId = mTextModel.backendId;

                    model.end = System.currentTimeMillis() / 1000;
                    model.persist();
                }

                @Override
                public void onDataReceived() {
                    SynchronizationService.startSync(true);
                }
            }.execute(TextReadingFragment.this);

        }

    }
}
