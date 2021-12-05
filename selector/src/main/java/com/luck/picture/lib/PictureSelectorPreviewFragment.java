package com.luck.picture.lib;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.luck.picture.lib.adapter.PicturePreviewAdapter;
import com.luck.picture.lib.adapter.holder.PreviewGalleryAdapter;
import com.luck.picture.lib.basic.PictureCommonFragment;
import com.luck.picture.lib.basic.PictureSelectorSupporterActivity;
import com.luck.picture.lib.config.Crop;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.config.SelectModeConfig;
import com.luck.picture.lib.decoration.GridSpacingItemDecoration;
import com.luck.picture.lib.decoration.ViewPage2ItemDecoration;
import com.luck.picture.lib.decoration.WrapContentLinearLayoutManager;
import com.luck.picture.lib.dialog.PictureCommonDialog;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.LocalMediaFolder;
import com.luck.picture.lib.interfaces.OnCallbackListener;
import com.luck.picture.lib.interfaces.OnQueryAlbumListener;
import com.luck.picture.lib.interfaces.OnQueryDataResultListener;
import com.luck.picture.lib.loader.LocalMediaLoader;
import com.luck.picture.lib.loader.LocalMediaPageLoader;
import com.luck.picture.lib.manager.SelectedManager;
import com.luck.picture.lib.style.SelectMainStyle;
import com.luck.picture.lib.style.TitleBarStyle;
import com.luck.picture.lib.utils.ActivityCompatHelper;
import com.luck.picture.lib.utils.DensityUtil;
import com.luck.picture.lib.utils.DownloadFileUtils;
import com.luck.picture.lib.utils.StyleUtils;
import com.luck.picture.lib.utils.ValueOf;
import com.luck.picture.lib.widget.BottomNavBar;
import com.luck.picture.lib.widget.CompleteSelectView;
import com.luck.picture.lib.widget.PreviewBottomNavBar;
import com.luck.picture.lib.widget.PreviewTitleBar;
import com.luck.picture.lib.widget.TitleBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author：luck
 * @date：2021/11/18 10:13 下午
 * @describe：PictureSelectorPreviewFragment
 */
public class PictureSelectorPreviewFragment extends PictureCommonFragment {
    public static final String TAG = PictureSelectorPreviewFragment.class.getSimpleName();

    public static PictureSelectorPreviewFragment newInstance() {
        PictureSelectorPreviewFragment fragment = new PictureSelectorPreviewFragment();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    private List<LocalMedia> mData = new ArrayList<>();

    private PreviewTitleBar titleBar;

    private PreviewBottomNavBar bottomNarBar;

    private ViewPager2 viewPager;

    private PicturePreviewAdapter viewPageAdapter;

    /**
     * if there more
     */
    protected boolean isHasMore = true;

    private int curPosition;

    private boolean isBottomPreview;

    private String currentAlbum;

    private boolean isShowCamera;

    /**
     * 是否外部预览进来
     */
    private boolean isExternalPreview;

    /**
     * 外部预览是否支持删除
     */
    private boolean isDisplayDelete;

    private int totalNum;

    private int screenWidth;

    private boolean isTransformPage = false;

    private long mBucketId = -1;

    private TextView tvSelected;

    private TextView tvSelectedWord;

    private View selectClickArea;

    private CompleteSelectView completeSelectView;

    private RecyclerView mGalleryRecycle;

    private PreviewGalleryAdapter mGalleryAdapter;

    /**
     * 内部预览
     *
     * @param isBottomPreview 是否顶部预览进来的
     * @param currentAlbum    当前预览的目录
     * @param isShowCamera    是否有显示拍照图标
     * @param position        预览下标
     * @param totalNum        当前预览总数
     * @param page            当前页码
     * @param currentBucketId 当前相册目录id
     * @param data            预览数据源
     */
    public void setInternalPreviewData(boolean isBottomPreview, String currentAlbum, boolean isShowCamera,
                                       int position, int totalNum, int page, long currentBucketId,
                                       List<LocalMedia> data) {
        this.mPage = page;
        this.mBucketId = currentBucketId;
        this.mData = data;
        this.totalNum = totalNum;
        this.curPosition = position;
        this.currentAlbum = currentAlbum;
        this.isShowCamera = isShowCamera;
        this.isBottomPreview = isBottomPreview;
    }

    /**
     * 外部预览
     *
     * @param position        预览下标
     * @param totalNum        当前预览总数
     * @param data            预览数据源
     * @param isDisplayDelete 是否显示删除按钮
     */
    public void setExternalPreviewData(int position, int totalNum, List<LocalMedia> data, boolean isDisplayDelete) {
        this.mData = data;
        this.totalNum = totalNum;
        this.curPosition = position;
        this.isDisplayDelete = isDisplayDelete;
        this.isExternalPreview = true;
    }

    @Override
    public int getResourceId() {
        return R.layout.ps_fragment_preview;
    }

    @Override
    public void onSelectedChange(boolean isAddRemove, LocalMedia currentMedia) {
        // 更新TitleBar和BottomNarBar选择态
        tvSelected.setSelected(SelectedManager.getSelectedResult().contains(currentMedia));
        bottomNarBar.setSelectedChange();
        completeSelectView.setSelectedChange(true);
        notifySelectNumberStyle(currentMedia);
        notifyPreviewGalleryData(isAddRemove, currentMedia);
    }

    @Override
    public void onCheckOriginalChange() {
        bottomNarBar.setOriginalCheck();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            mPage = savedInstanceState.getInt(PictureConfig.EXTRA_CURRENT_PAGE, 1);
            mBucketId = savedInstanceState.getLong(PictureConfig.EXTRA_CURRENT_BUCKET_ID, -1);
            curPosition = savedInstanceState.getInt(PictureConfig.EXTRA_PREVIEW_CURRENT_POSITION, curPosition);
            isShowCamera = savedInstanceState.getBoolean(PictureConfig.EXTRA_DISPLAY_CAMERA, isShowCamera);
            totalNum = savedInstanceState.getInt(PictureConfig.EXTRA_PREVIEW_CURRENT_ALBUM_TOTAL, totalNum);
            isExternalPreview = savedInstanceState.getBoolean(PictureConfig.EXTRA_EXTERNAL_PREVIEW, isExternalPreview);
            isDisplayDelete = savedInstanceState.getBoolean(PictureConfig.EXTRA_EXTERNAL_PREVIEW_DISPLAY_DELETE, isDisplayDelete);
            isBottomPreview = savedInstanceState.getBoolean(PictureConfig.EXTRA_BOTTOM_PREVIEW, isBottomPreview);
        }
        screenWidth = DensityUtil.getScreenWidth(getContext());
        titleBar = view.findViewById(R.id.title_bar);
        tvSelected = view.findViewById(R.id.ps_tv_selected);
        tvSelectedWord = view.findViewById(R.id.ps_tv_selected_word);
        selectClickArea = view.findViewById(R.id.select_click_area);
        completeSelectView = view.findViewById(R.id.ps_complete_select);
        viewPager = view.findViewById(R.id.preview_pager);
        bottomNarBar = view.findViewById(R.id.bottom_nar_bar);
        initTitleBar();
        if (isExternalPreview) {
            if (savedInstanceState != null || mData.size() == 0) {
                mData = new ArrayList<>(SelectedManager.getSelectedPreviewResult());
            }
            SelectedManager.clearExternalPreviewData();
            initViewPagerData();
            externalPreviewStyle();
            initViewPagerData();
        } else {
            initLoader();
            initBottomNavBar();
            initPreviewSelectGallery(view);
            initComplete();
            if (savedInstanceState != null && mData.size() == 0) {
                // 这种情况就是内存不足导致页面被回收后的补全逻辑，让其恢复到回收前的样子
                if (isBottomPreview) {
                    mData = new ArrayList<>(SelectedManager.getSelectedResult());
                    initViewPagerData();
                } else {
                    if (config.isPageStrategy) {
                        loadData(mPage * config.pageSize);
                    } else {
                        // 就算不是分页模式也强行先使用LocalMediaPageLoader模式获取数据
                        mLoader = new LocalMediaPageLoader(getContext(), config);
                        loadData(totalNum);
                    }
                }
            } else {
                initViewPagerData();
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(PictureConfig.EXTRA_CURRENT_PAGE, mPage);
        outState.putLong(PictureConfig.EXTRA_CURRENT_BUCKET_ID, mBucketId);
        outState.putInt(PictureConfig.EXTRA_PREVIEW_CURRENT_POSITION, curPosition);
        outState.putInt(PictureConfig.EXTRA_PREVIEW_CURRENT_ALBUM_TOTAL, totalNum);
        outState.putBoolean(PictureConfig.EXTRA_EXTERNAL_PREVIEW, isExternalPreview);
        outState.putBoolean(PictureConfig.EXTRA_EXTERNAL_PREVIEW_DISPLAY_DELETE, isDisplayDelete);
        outState.putBoolean(PictureConfig.EXTRA_DISPLAY_CAMERA, isShowCamera);
        outState.putBoolean(PictureConfig.EXTRA_BOTTOM_PREVIEW, isBottomPreview);
        if (isExternalPreview) {
            SelectedManager.addSelectedPreviewResult(mData);
        }
    }

    @Override
    public void subSelectPosition(boolean isRefreshAdapter) {
        if (PictureSelectionConfig.selectorStyle.getSelectMainStyle().isPreviewSelectNumberStyle()) {
            if (PictureSelectionConfig.selectorStyle.getSelectMainStyle().isSelectNumberStyle()) {
                for (int index = 0; index < SelectedManager.getCount(); index++) {
                    LocalMedia media = SelectedManager.getSelectedResult().get(index);
                    media.setNum(index + 1);
                }
            }
        }
    }


    /**
     * init LocalMedia Loader
     */
    protected void initLoader() {
        if (config.isPageStrategy) {
            mLoader = new LocalMediaPageLoader(getContext(), config);
        } else {
            mLoader = new LocalMediaLoader(getContext(), config);
        }
    }

    /**
     * 加载数据
     */
    private void loadData(int pageSize) {
        if (config.isOnlySandboxDir) {
            if (PictureSelectionConfig.loaderDataEngine != null) {
                PictureSelectionConfig.loaderDataEngine.loadOnlyInAppDirAllMediaData(getContext(),
                        new OnQueryAlbumListener<LocalMediaFolder>() {
                            @Override
                            public void onComplete(LocalMediaFolder folder) {
                                handleLoadData(folder.getData());
                            }
                        });
            } else {
                mLoader.loadOnlyInAppDirAllMedia(new OnQueryAlbumListener<LocalMediaFolder>() {
                    @Override
                    public void onComplete(LocalMediaFolder folder) {
                        handleLoadData(folder.getData());
                    }
                });
            }
        } else {
            if (PictureSelectionConfig.loaderDataEngine != null) {
                PictureSelectionConfig.loaderDataEngine.loadFirstPageMediaData(getContext(),
                        mBucketId, 1, pageSize, new OnQueryDataResultListener<LocalMedia>() {
                            @Override
                            public void onComplete(List<LocalMedia> result, boolean isHasMore) {
                                handleLoadData(result);
                            }
                        });
            } else {
                mLoader.loadFirstPageMedia(mBucketId, pageSize, new OnQueryDataResultListener<LocalMedia>() {
                    @Override
                    public void onComplete(List<LocalMedia> result, boolean isHasMore) {
                        handleLoadData(result);
                    }
                });
            }
        }
    }

    private void handleLoadData(List<LocalMedia> result) {
        if (ActivityCompatHelper.isDestroy(getActivity())) {
            return;
        }
        mData = result;
        if (mData.size() == 0) {
            iBridgePictureBehavior.onFinish();
            return;
        }
        // 这里的作用主要是防止内存不足情况下重新load了数据，此时LocalMedia是没有position的
        // 但如果此时你选中或取消一个结果,PictureSelectorFragment列表页 notifyItemChanged下标会不对
        int position = isShowCamera ? 0 : -1;
        for (int i = 0; i < mData.size(); i++) {
            position++;
            mData.get(i).setPosition(position);
        }
        initViewPagerData();
    }

    /**
     * 加载更多
     */
    private void loadMoreData() {
        mPage++;
        if (PictureSelectionConfig.loaderDataEngine != null) {
            PictureSelectionConfig.loaderDataEngine.loadMoreMediaData(getContext(), mBucketId, mPage,
                    config.pageSize, config.pageSize, new OnQueryDataResultListener<LocalMedia>() {
                        @Override
                        public void onComplete(List<LocalMedia> result, boolean isHasMore) {
                            handleMoreData(result, isHasMore);
                        }
                    });
        } else {
            mLoader.loadPageMediaData(mBucketId, mPage, config.pageSize,new OnQueryDataResultListener<LocalMedia>() {
                        @Override
                        public void onComplete(List<LocalMedia> result, boolean isHasMore) {
                            handleMoreData(result, isHasMore);
                        }
                    });
        }
    }

    private void handleMoreData(List<LocalMedia> result, boolean isHasMore) {
        if (ActivityCompatHelper.isDestroy(getActivity())) {
            return;
        }
        PictureSelectorPreviewFragment.this.isHasMore = isHasMore;
        if (isHasMore) {
            if (result.size() > 0) {
                int oldStartPosition = mData.size();
                mData.addAll(result);
                int itemCount = mData.size();
                viewPageAdapter.notifyItemRangeChanged(oldStartPosition, itemCount);
            } else {
                loadMoreData();
            }
        }
    }


    private void initComplete() {
        SelectMainStyle selectMainStyle = PictureSelectionConfig.selectorStyle.getSelectMainStyle();

        if (StyleUtils.checkStyleValidity(selectMainStyle.getPreviewSelectBackground())) {
            tvSelected.setBackgroundResource(selectMainStyle.getPreviewSelectBackground());
        } else if (StyleUtils.checkStyleValidity(selectMainStyle.getSelectBackground())) {
            tvSelected.setBackgroundResource(selectMainStyle.getSelectBackground());
        }
        if (StyleUtils.checkTextValidity(selectMainStyle.getPreviewSelectText())) {
            tvSelectedWord.setText(selectMainStyle.getPreviewSelectText());
        } else {
            tvSelectedWord.setText("");
        }
        if (StyleUtils.checkSizeValidity(selectMainStyle.getPreviewSelectTextSize())) {
            tvSelectedWord.setTextSize(selectMainStyle.getPreviewSelectTextSize());
        }

        if (StyleUtils.checkStyleValidity(selectMainStyle.getPreviewSelectTextColor())) {
            tvSelectedWord.setTextColor(selectMainStyle.getPreviewSelectTextColor());
        }

        if (StyleUtils.checkSizeValidity(selectMainStyle.getPreviewSelectMarginRight())) {
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) tvSelected.getLayoutParams();
            layoutParams.rightMargin = selectMainStyle.getPreviewSelectMarginRight();
        }

        completeSelectView.setCompleteSelectViewStyle();
        if (selectMainStyle.isCompleteSelectRelativeTop()) {
            ((ConstraintLayout.LayoutParams) completeSelectView
                    .getLayoutParams()).topToTop = R.id.title_bar;
            ((ConstraintLayout.LayoutParams) completeSelectView
                    .getLayoutParams()).bottomToBottom = R.id.title_bar;

        }
        if (selectMainStyle.isPreviewSelectRelativeBottom()) {
            ((ConstraintLayout.LayoutParams) tvSelected
                    .getLayoutParams()).topToTop = R.id.bottom_nar_bar;
            ((ConstraintLayout.LayoutParams) tvSelected
                    .getLayoutParams()).bottomToBottom = R.id.bottom_nar_bar;

            ((ConstraintLayout.LayoutParams) tvSelectedWord
                    .getLayoutParams()).topToTop = R.id.bottom_nar_bar;
            ((ConstraintLayout.LayoutParams) tvSelectedWord
                    .getLayoutParams()).bottomToBottom = R.id.bottom_nar_bar;

            ((ConstraintLayout.LayoutParams) selectClickArea
                    .getLayoutParams()).topToTop = R.id.bottom_nar_bar;
            ((ConstraintLayout.LayoutParams) selectClickArea
                    .getLayoutParams()).bottomToBottom = R.id.bottom_nar_bar;

        }
        completeSelectView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isComplete = true;
                if (selectMainStyle.isCompleteSelectRelativeTop() && SelectedManager.getCount() == 0) {
                    isComplete = confirmSelect(mData.get(viewPager.getCurrentItem()), false)
                            == SelectedManager.ADD_SUCCESS;
                }
                if (isComplete) {
                    dispatchTransformResult();
                }
            }
        });
    }


    private void initTitleBar() {
        if (PictureSelectionConfig.selectorStyle.getTitleBarStyle().isHideTitleBar()) {
            titleBar.setVisibility(View.GONE);
        }
        titleBar.setTitleBarStyle();
        titleBar.setOnTitleBarListener(new TitleBar.OnTitleBarListener() {
            @Override
            public void onBackPressed() {
                if (isExternalPreview) {
                    handleExternalPreviewBack();
                } else {
                    iBridgePictureBehavior.onFinish();
                }
            }
        });
        titleBar.setTitle((curPosition + 1) + "/" + totalNum);
        selectClickArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isExternalPreview) {
                    deletePreview();
                } else {
                    LocalMedia currentMedia = mData.get(viewPager.getCurrentItem());
                    int selectResultCode = confirmSelect(currentMedia, tvSelected.isSelected());
                    if (selectResultCode == SelectedManager.ADD_SUCCESS) {
                        tvSelected.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.ps_anim_modal_in));
                    }
                }
            }
        });
        tvSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectClickArea.performClick();
            }
        });
    }

    private void initPreviewSelectGallery(View group) {
        SelectMainStyle selectMainStyle = PictureSelectionConfig.selectorStyle.getSelectMainStyle();
        if (selectMainStyle.isPreviewDisplaySelectGallery()) {
            if (group instanceof ConstraintLayout) {
                mGalleryRecycle = new RecyclerView(getContext());
                if (StyleUtils.checkStyleValidity(selectMainStyle.getAdapterPreviewGalleryBackgroundResource())) {
                    mGalleryRecycle.setBackgroundResource(selectMainStyle.getAdapterPreviewGalleryBackgroundResource());
                } else {
                    mGalleryRecycle.setBackgroundResource(R.drawable.ps_preview_gallery_bg);
                }
                ((ConstraintLayout) group).addView(mGalleryRecycle);
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mGalleryRecycle.getLayoutParams();
                params.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
                params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
                params.bottomToTop = R.id.bottom_nar_bar;
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                WrapContentLinearLayoutManager layoutManager = new WrapContentLinearLayoutManager(getContext());
                layoutManager.setOrientation(WrapContentLinearLayoutManager.HORIZONTAL);
                mGalleryRecycle.setLayoutManager(layoutManager);
                mGalleryRecycle.addItemDecoration(new GridSpacingItemDecoration(Integer.MAX_VALUE,
                        DensityUtil.dip2px(getContext(), 6), true));
                mGalleryAdapter = new PreviewGalleryAdapter(SelectedManager.getSelectedResult());
                mGalleryAdapter.isSelectMedia(mData.get(curPosition));
                mGalleryRecycle.setAdapter(mGalleryAdapter);
                mGalleryAdapter.setItemClickListener(new PreviewGalleryAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(int position, LocalMedia media, View v) {
                        if (isBottomPreview || TextUtils.equals(currentAlbum, getString(R.string.ps_camera_roll))
                                || TextUtils.equals(media.getParentFolderName(), currentAlbum)) {
                            int newPosition = isBottomPreview ? position : isShowCamera ? media.position - 1 : media.position;
                            viewPager.setCurrentItem(newPosition, false);
                        }
                    }
                });

                if (SelectedManager.getCount() > 0) {
                    mGalleryRecycle.setVisibility(View.VISIBLE);
                } else {
                    mGalleryRecycle.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    /**
     * 调用了startPreview预览逻辑
     */
    @SuppressLint("NotifyDataSetChanged")
    private void deletePreview() {
        if (PictureSelectionConfig.previewEventListener != null) {
            PictureSelectionConfig.previewEventListener.onPreviewDelete(viewPager.getCurrentItem());
            int currentItem = viewPager.getCurrentItem();
            mData.remove(currentItem);
            if (mData.size() == 0) {
                handleExternalPreviewBack();
                return;
            }
            titleBar.setTitle(getString(R.string.ps_preview_image_num,
                    curPosition + 1, mData.size()));
            totalNum = mData.size();
            curPosition = currentItem;
            viewPager.setCurrentItem(curPosition, false);
            isTransformPage = true;
            viewPager.setPageTransformer(new ViewPager2.PageTransformer() {
                @Override
                public void transformPage(@NonNull View page, float position) {
                    if (isTransformPage) {
                        ObjectAnimator animator = ObjectAnimator.ofFloat(page, "alpha", 0F, 1F);
                        animator.setDuration(450);
                        animator.setInterpolator(new LinearInterpolator());
                        animator.start();
                        isTransformPage = false;
                    }
                }
            });
            viewPageAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 处理外部预览返回处理
     */
    private void handleExternalPreviewBack() {
        if (!ActivityCompatHelper.isDestroy(getActivity())) {
            if (getActivity() instanceof PictureSelectorSupporterActivity) {
                iBridgePictureBehavior.onFinish();
            } else {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        }
    }


    private void initBottomNavBar() {
        bottomNarBar.setBottomNavBarStyle();
        bottomNarBar.setSelectedChange();
        bottomNarBar.setOnBottomNavBarListener(new BottomNavBar.OnBottomNavBarListener() {

            @Override
            public void onEditImage() {
                if (PictureSelectionConfig.editMediaEventListener != null) {
                    LocalMedia media = mData.get(viewPager.getCurrentItem());
                    PictureSelectionConfig.editMediaEventListener
                            .onStartMediaEdit(PictureSelectorPreviewFragment.this, media,
                                    Crop.REQUEST_EDIT_CROP);
                }
            }

            @Override
            public void onCheckOriginalChange() {
                sendSelectedOriginalChangeEvent();
            }
        });
    }

    /**
     * 外部预览的样式
     */
    private void externalPreviewStyle() {
        TitleBarStyle titleBarStyle = PictureSelectionConfig.selectorStyle.getTitleBarStyle();
        int deleteBackgroundResource = titleBarStyle.getPreviewDeleteBackgroundResource();
        if (StyleUtils.checkStyleValidity(deleteBackgroundResource)) {
            tvSelected.setBackgroundResource(deleteBackgroundResource);
        } else {
            tvSelected.setBackgroundResource(R.drawable.ps_ic_delete);
        }
        tvSelected.setVisibility(isDisplayDelete ? View.VISIBLE : View.GONE);
        tvSelected.setVisibility(isDisplayDelete ? View.VISIBLE : View.GONE);
        bottomNarBar.setVisibility(View.GONE);
        completeSelectView.setVisibility(View.GONE);
    }

    private void initViewPagerData() {
        viewPageAdapter = new PicturePreviewAdapter(getContext(), mData, config);
        viewPageAdapter.setOnPreviewEventListener(new PicturePreviewAdapter.OnPreviewEventListener() {
            @Override
            public void onBackPressed() {
                if (isExternalPreview) {
                    handleExternalPreviewBack();
                } else {
                    iBridgePictureBehavior.onFinish();
                }
            }

            @Override
            public void onPreviewVideoTitle(String videoName) {
                if (TextUtils.isEmpty(videoName)) {
                    titleBar.setTitle((curPosition + 1) + "/" + totalNum);
                } else {
                    titleBar.setTitle(videoName);
                }
            }

            @Override
            public void onLongPressDownload(LocalMedia media) {
                if (isExternalPreview) {
                    onExternalLongPressDownload(media);
                }
            }
        });
        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        viewPager.addItemDecoration(new ViewPage2ItemDecoration(1,
                DensityUtil.dip2px(Objects.requireNonNull(getActivity()), 1)));
        viewPager.setAdapter(viewPageAdapter);
        viewPager.setCurrentItem(curPosition, false);
        tvSelected.setSelected(SelectedManager.getSelectedResult().contains(mData.get(viewPager.getCurrentItem())));
        completeSelectView.setSelectedChange(true);
        viewPager.registerOnPageChangeCallback(pageChangeCallback);
        subSelectPosition(false);
        notifySelectNumberStyle(mData.get(curPosition));
    }

    /**
     * 外部预览长按下载
     *
     * @param media
     */
    private void onExternalLongPressDownload(LocalMedia media) {
        if (PictureSelectionConfig.previewEventListener != null) {
            if (!PictureSelectionConfig.previewEventListener.onLongPressDownload(media)) {
                PictureCommonDialog dialog = PictureCommonDialog.showDialog(getContext(),
                        getContext().getString(R.string.ps_prompt),
                        getContext().getString(R.string.ps_prompt_content));
                dialog.setOnDialogEventListener(new PictureCommonDialog.OnDialogEventListener() {
                    @Override
                    public void onConfirm() {
                        String path;
                        if (TextUtils.isEmpty(media.getSandboxPath())) {
                            path = media.getPath();
                        } else {
                            path = media.getSandboxPath();
                        }
                        if (PictureMimeType.isHasHttp(path)) {
                            showLoading();
                        }
                        DownloadFileUtils.saveLocalFile(getContext(),
                                path, media.getFileName(), media.getMimeType(), new OnCallbackListener<String>() {
                                    @Override
                                    public void onCall(String realPath) {
                                        dismissLoading();
                                        if (TextUtils.isEmpty(realPath)) {
                                            String errorMsg;
                                            if (PictureMimeType.isHasVideo(media.getMimeType())) {
                                                errorMsg = getString(R.string.ps_save_video_error);
                                            } else {
                                                errorMsg = getString(R.string.ps_save_image_error);
                                            }
                                            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(getContext(),
                                                    getString(R.string.ps_save_success) + "\n" + realPath,
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                    }
                });
            }
        }
    }

    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            LocalMedia currentMedia = positionOffsetPixels < screenWidth / 2 ? mData.get(position) : mData.get(position + 1);
            tvSelected.setSelected(isSelected(currentMedia));
            notifySelectNumberStyle(currentMedia);
        }

        @Override
        public void onPageSelected(int position) {
            curPosition = position;
            titleBar.setTitle((curPosition + 1) + "/" + totalNum);
            LocalMedia currentMedia = mData.get(position);
            notifySelectNumberStyle(currentMedia);
            if (mGalleryAdapter != null) {
                mGalleryAdapter.isSelectMedia(currentMedia);
            }
            bottomNarBar.isDisplayEditor(PictureMimeType.isHasVideo(currentMedia.getMimeType()));
            if (!isExternalPreview && !isBottomPreview && !config.isOnlySandboxDir) {
                if (config.isPageStrategy) {
                    if (isHasMore) {
                        if (position == (viewPageAdapter.getItemCount() - 1) - PictureConfig.MIN_PAGE_SIZE
                                || position == viewPageAdapter.getItemCount() - 1) {
                            loadMoreData();
                        }
                    }
                }
            }
        }
    };

    /**
     * 刷新画廊数据
     */
    private void notifyPreviewGalleryData(boolean isAddRemove, LocalMedia currentMedia) {
        if (mGalleryAdapter != null && PictureSelectionConfig.selectorStyle
                .getSelectMainStyle().isPreviewDisplaySelectGallery()) {
            if (mGalleryRecycle.getVisibility() == View.INVISIBLE) {
                mGalleryRecycle.setVisibility(View.VISIBLE);
            }
            if (isAddRemove) {
                if (config.selectionMode == SelectModeConfig.SINGLE) {
                    mGalleryAdapter.clear();
                }
                mGalleryAdapter.addGalleryData(isBottomPreview, currentMedia);
            } else {
                mGalleryAdapter.removeGalleryData(isBottomPreview && totalNum > 1, currentMedia);
                if (mGalleryAdapter.getItemCount() == 0) {
                    mGalleryRecycle.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    /**
     * 对选择数量进行编号排序
     */
    public void notifySelectNumberStyle(LocalMedia currentMedia) {
        if (PictureSelectionConfig.selectorStyle.getSelectMainStyle().isPreviewSelectNumberStyle()) {
            if (PictureSelectionConfig.selectorStyle.getSelectMainStyle().isSelectNumberStyle()) {
                tvSelected.setText("");
                for (int i = 0; i < SelectedManager.getCount(); i++) {
                    LocalMedia media = SelectedManager.getSelectedResult().get(i);
                    if (TextUtils.equals(media.getPath(), currentMedia.getPath())
                            || media.getId() == currentMedia.getId()) {
                        currentMedia.setNum(media.getNum());
                        media.setPosition(currentMedia.getPosition());
                        tvSelected.setText(ValueOf.toString(currentMedia.getNum()));
                    }
                }
            }
        }
    }

    /**
     * 当前图片是否选中
     *
     * @param media
     * @return
     */
    protected boolean isSelected(LocalMedia media) {
        return SelectedManager.getSelectedResult().contains(media);
    }

    @Override
    public void onEditMedia(Intent data) {
        LocalMedia media = mData.get(viewPager.getCurrentItem());
        Uri output = Crop.getOutput(data);
        media.setCutPath(output != null ? output.getPath() : "");
        media.setCropImageWidth(Crop.getOutputImageWidth(data));
        media.setCropImageHeight(Crop.getOutputImageHeight(data));
        media.setCropOffsetX(Crop.getOutputImageOffsetX(data));
        media.setCropOffsetY(Crop.getOutputImageOffsetY(data));
        media.setCropResultAspectRatio(Crop.getOutputCropAspectRatio(data));
        media.setCut(!TextUtils.isEmpty(media.getCutPath()));
        media.setCustomData(Crop.getOutputCustomExtraData(data));
        media.setEditorImage(media.isCut());
        media.setSandboxPath(media.getCutPath());
        if (!SelectedManager.getSelectedResult().contains(media)) {
            confirmSelect(media, false);
        }
        viewPageAdapter.notifyItemChanged(viewPager.getCurrentItem());
    }

    @Override
    public void onDestroy() {
        viewPageAdapter.destroyCurrentVideoHolder();
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        if (isExternalPreview) {
            PictureSelectionConfig.destroy();
        }
        super.onDestroy();
    }
}