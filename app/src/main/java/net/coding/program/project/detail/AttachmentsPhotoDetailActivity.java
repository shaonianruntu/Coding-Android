package net.coding.program.project.detail;

import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import net.coding.program.pickphoto.detail.ImagePagerFragment;
import net.coding.program.R;
import net.coding.program.common.widget.BottomToolBar;
import net.coding.program.common.model.AttachmentFileObject;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.io.File;

@EActivity(R.layout.activity_attachments_photo_detail)
public class AttachmentsPhotoDetailActivity extends AttachmentsDetailBaseActivity {

    @ViewById
    SubsamplingScaleImageView imageView;

    @ViewById
    View layout_image_prototype, clickImagePrototype;

    @ViewById
    BottomToolBar bottomToolBar;

    @ViewById
    ProgressBar progressBar;

    @ViewById
    View ivDownloadCancel;

    @ViewById
    TextView progressBarText;

    @AfterViews
    protected final void initAttachmentsPhotoDetailActivity() {
        updateDisplay();
        bottomToolBar.setClick(clickBottomBar);

        bindUIDownload(false);
    }

    private void updateDisplay() {
        if (mExtraFile != null) {
            String filePath = "file://" + mExtraFile.getPath();

            imageView.setImage(ImageSource.uri(filePath));
            layout_image_prototype.setVisibility(View.GONE);
            bottomToolBar.setVisibility(View.VISIBLE);
            findViewById(R.id.bottomPanel).setVisibility(View.GONE);

        } else if (mFile.exists()) {
            String filePath = "file://" + mFile.getPath();
            imageView.setImage(ImageSource.uri(filePath));
            layout_image_prototype.setVisibility(View.GONE);
            bottomToolBar.setVisibility(View.VISIBLE);
        } else {
            getImageLoad().imageLoader.loadImage(mAttachmentFileObject.owner_preview, ImagePagerFragment.optionsImage, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    File file = getImageLoad().imageLoader.getDiskCache().get(imageUri);
                    if (file != null) {
                        imageView.setImage(ImageSource.uri(file.getAbsolutePath()));
                        layout_image_prototype.setVisibility(View.VISIBLE);
                        bottomToolBar.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

    }

    @Override
    protected int getMenuResourceId() {
        return R.menu.project_attachment_photo;
    }

    @Click
    protected void clickImagePrototype() {
        action_download();
        showMiddleToast("开始下载");

        bindUIDownload(true);
    }

    private void bindUIDownload(boolean downloading) {
        if (downloading) {
            progressBar.setVisibility(View.VISIBLE);
            progressBarText.setVisibility(View.VISIBLE);
            ivDownloadCancel.setVisibility(View.VISIBLE);
            clickImagePrototype.setVisibility(View.INVISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            progressBarText.setVisibility(View.INVISIBLE);
            ivDownloadCancel.setVisibility(View.INVISIBLE);
            clickImagePrototype.setVisibility(View.VISIBLE);
        }
    }

    @Click
    void ivDownloadCancel() {
        bindUIDownload(false);
    }

    @Override
    protected void onDownloadProgress(int progress) {
        progressBar.setProgress(progress);
    }

    @Override
    protected void onDownloadFinish(boolean success) {
        showProgressBar(false);
        if (success) {
            mAttachmentFileObject.isDownload = true;
            setResult(RESULT_OK);
            updateDisplay();
        } else {
            bindUIDownload(false);
        }
    }
}
