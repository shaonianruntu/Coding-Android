package net.coding.program.project.detail.merge;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.loopj.android.http.RequestParams;

import net.coding.program.CodingGlobal;
import net.coding.program.R;
import net.coding.program.common.Global;
import net.coding.program.common.GlobalCommon;
import net.coding.program.common.GlobalData;
import net.coding.program.common.LongClickLinkMovementMethod;
import net.coding.program.common.MyImageGetter;
import net.coding.program.common.RedPointTip;
import net.coding.program.common.base.MyJsonResponse;
import net.coding.program.common.comment.BaseCommentParam;
import net.coding.program.common.model.BaseComment;
import net.coding.program.common.model.DiffFile;
import net.coding.program.common.model.DynamicObject;
import net.coding.program.common.model.Merge;
import net.coding.program.common.model.MergeDetail;
import net.coding.program.common.model.ProjectObject;
import net.coding.program.common.model.RefResourceObject;
import net.coding.program.common.model.RequestData;
import net.coding.program.common.network.MyAsyncHttpClient;
import net.coding.program.common.network.NetworkImpl;
import net.coding.program.common.ui.CodingToolbarBackActivity;
import net.coding.program.common.umeng.UmengEvent;
import net.coding.program.common.util.DensityUtil;
import net.coding.program.common.widget.DataAdapter;
import net.coding.program.common.widget.ListItem1;
import net.coding.program.pickphoto.ClickSmallImage;
import net.coding.program.project.detail.MembersSelectActivity_;
import net.coding.program.project.git.CommitListActivity_;
import net.coding.program.route.BlankViewDisplay;
import net.coding.program.route.URLSpanNoUnderline;
import net.coding.program.task.add.CommentHolder;
import net.coding.program.task.add.RefResourceActivity_;
import net.coding.program.task.add.TaskParam;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.ViewById;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import de.hdodenhof.circleimageview.CircleImageView;

@EActivity(R.layout.activity_merge_detail)
public class MergeDetailActivity extends CodingToolbarBackActivity {

    public static final int RESULT_COMMENT = 1;
    public static final int RESULT_MERGE = 2;
    public static final int RESULT_RESUSE_REFRESOURCE = 3;

    //    private static final String HOST_MERGE_COMMENTS = "HOST_MERGE_COMMENTS";
    private static final String HOST_MERGE_REFUSE = "HOST_MERGE_REFUSE";
    private static final String HOST_MERGE_CANNEL = "HOST_MERGE_CANNEL";
    private static final String HOST_MERGE_DETAIL = "HOST_MERGE_DETAIL";
    private static final String HOST_DELETE_COMMENT = "HOST_DELETE_COMMENT";

    private static final String TAG_REVIEW_GOOD = "TAG_REVIEW_GOOD";
    private final ClickSmallImage onClickImage = new ClickSmallImage(this);
    @Extra
    Merge mMerge;
    @Extra
    String mMergeUrl;
    @ViewById
    View actionLayout, actionAccept, actionRefuse, actionCancel, actionAuth, actionCancelAuth, blankLayout;
    @ViewById
    ListView listView;
    DataAdapter mAdapter;
    MyImageGetter myImageGetter = new MyImageGetter(this);
    Comparator<DynamicObject.DynamicBaseObject> mDynamicSorter = (lhs, rhs) -> (int) (lhs.created_at - rhs.created_at);
    View.OnClickListener mOnClickItem = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final BaseComment item = (BaseComment) v.getTag();
            if (item.isMy()) {
                showDialog("merge", "删除评论?", (dialog, which) -> {
                    String url = mMerge.getHttpDeleteComment(item.id);
                    deleteNetwork(url, HOST_DELETE_COMMENT, item);
                });
            } else {
                String name = item.owner.name;
                CommentActivity.CommentParam param = createParam(name);
                CommentActivity_.intent(MergeDetailActivity.this).mParam(param).startForResult(RESULT_COMMENT);
            }
        }
    };
    private ArrayList<RefResourceObject> refResourceList = new ArrayList<>();
    private ArrayList<Merge.Reviewer> reviewerList = new ArrayList<>();
    private ArrayList<DynamicObject.DynamicMergeRequest> dynamicList = new ArrayList<>();
    private MergeDetail mMergeDetail;
    private WebView contentWeb;
    private View.OnClickListener mOnClickComment = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final DynamicObject.DynamicMergeRequest comment = (DynamicObject.DynamicMergeRequest) v.getTag();
            if (comment == null) return;

            if (comment.user.global_key.equals(GlobalData.sUserObject.global_key)) {
                showDialog("Merge Request", "删除评论？", (dialog, which) -> {
                    String url = mMerge.getHttpDeleteComment(comment.id);
                    deleteNetwork(url, HOST_DELETE_COMMENT, comment);
                });
            } else {
                String atName = String.format("%s", comment.user.global_key);
                CommentActivity.CommentParam param = createParam(atName);
                CommentActivity_.intent(MergeDetailActivity.this).mParam(param).startForResult(RESULT_COMMENT);
            }
        }
    };
    DataAdapter<DynamicObject.DynamicMergeRequest> commentAdpter = new DataAdapter<DynamicObject.DynamicMergeRequest>() {
        @Override
        public int getItemViewType(int position) {
            DynamicObject.DynamicMergeRequest data =
                    (DynamicObject.DynamicMergeRequest) getItem(position);
            if (data.action.equals("comment")) {
                return 1;
            } else if (data.action.equals("comment_commit")) {
                return 2;
            } else {
                return 0;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int count = getCount();
            int type = getItemViewType(position);

            DynamicObject.DynamicMergeRequest data =
                    (DynamicObject.DynamicMergeRequest) getItem(position);

            DynamicHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(type == 1 ?
                        R.layout.activity_task_comment_much_image_task :
                        R.layout.task_list_item_dynamic, parent, false);
                holder = new DynamicHolder(convertView);
                convertView.setTag(R.id.Commentlayout, holder);
                CommentHolder commentHolder = new CommentHolder(convertView, mOnClickComment, myImageGetter, getImageLoad(), GlobalCommon.mOnClickUser, onClickImage);
                convertView.setTag(R.id.flowLayout, commentHolder);
            } else {
                holder = (DynamicHolder) convertView.getTag(R.id.Commentlayout);
            }

            holder.updateLine(position, count);


            if (type == 1) {
                CommentHolder commentHolder = (CommentHolder) convertView.getTag(R.id.flowLayout);
                commentHolder.setContent(data);
                return convertView;
            } else {
                holder.mContent.setText(data.title());
                int iconResId = data.action_icon;
                holder.mIcon.setImageResource(iconResId);
                if (type == 2) {
                    holder.mContent2.setVisibility(View.VISIBLE);
                    holder.mContent2.setText(data.content(myImageGetter));
                    holder.mContent2.setOnClickListener(v -> {
                        DiffFile.DiffSingleFile fileData =
                                ((DynamicObject.DynamicMergeRequestCommentCommit) data).getDiffSingleFile();
                        MergeFileDetailActivity_
                                .intent(MergeDetailActivity.this)
                                .mProjectPath(mMerge.getProjectPath())
                                .mergeIid(mMerge.getIid())
                                .mSingleFile(fileData)
                                .start();
                    });
                } else {
                    holder.mContent2.setVisibility(View.GONE);
                }
                holder.updateLine(position, count);

                return convertView;
            }
        }
    };

    private View.OnClickListener clickJumpReviewUsers = v -> {
        Intent intent = new Intent(MergeDetailActivity.this, MembersSelectActivity_.class);
        intent.putExtra("mMerge", mMerge);
        startActivityForResult(intent, MergeReviewerListFragment.RESULT_ADD_USER);
    };

    @AfterViews
    protected final void initMergeDetailActivity() {
        String httpReviewers;
        if (mMerge != null) {
            initByMereData();
            getNetwork(mMerge.getHttpDetail(), HOST_MERGE_DETAIL);
            httpReviewers = mMerge.getHttpReviewers();
        } else {
            showDialogLoading();
            String baseGit = URLSpanNoUnderline.generateAbsolute(mMergeUrl);
            getNetwork(baseGit + "/base", HOST_MERGE_DETAIL);
//            httpReviewers = baseGit + "/reviewers";
        }
    }

    @Nullable
    @Override
    protected ProjectObject getProject() {
        return null;
    }

    @Override
    protected String getProjectPath() {
        return mMerge.getProjectPath();
    }

    private void initByMereData() {
        setActionBarTitle(mMerge.getTitleIId());

        View mListHead = mInflater.inflate(R.layout.activity_merge_detail_head, null);
        initHead(mListHead);
        listView.addHeaderView(mListHead, null, false);
        View footer = mInflater.inflate(R.layout.activity_merge_detail_footer, null);

        footer.findViewById(R.id.gap_to_list).setVisibility(View.GONE);
        listView.addFooterView(footer, null, false);
        initFooter(footer);

        BaseCommentParam param = new BaseCommentParam(new ClickSmallImage(this), mOnClickItem, myImageGetter, getImageLoad(), GlobalCommon.mOnClickUser);
        mAdapter = commentAdpter; //new MergeRequestDynamicAdapter(this, myImageGetter);
//        mAdapter.setHasMore(false);

        listView.setAdapter(mAdapter);
//        listView.setAreHeadersSticky(false);
        listView.setDividerHeight(0);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DynamicObject.DynamicBaseObject activity =
                        (DynamicObject.DynamicBaseObject) mAdapter.getItem(position);
                if (activity.user.global_key.equals(GlobalData.sUserObject.global_key) && "comment".equals(activity.action)) {
                    showDialog(mMerge.getTitle(), "删除评论?", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String url = mMerge.getHttpDeleteComment(activity.id);
                            deleteNetwork(url, HOST_DELETE_COMMENT, activity);
                        }
                    });
                } else {
                    String name = activity.user.name;
                    CommentActivity.CommentParam param = createParam(name);
                    CommentActivity_.intent(MergeDetailActivity.this).mParam(param).startForResult(RESULT_COMMENT);
                }
            }
        });

        updateReviewer();

        if (mMerge == null || mMerge.isPull()) {
            findViewById(R.id.itemRefResource).setVisibility(View.GONE);
            return;
        } else {
            findViewById(R.id.itemRefResource).setVisibility(View.VISIBLE);
        }

        setActionStyle(false, false, false, false, false);

        refreshActivities();
    }

    private void updateBottomBarStyle() {
        if (mMergeDetail == null) {
            return;
        }

        boolean author_can_edit = mMergeDetail.isCanEditSrcBranch();
        boolean canEdit = mMergeDetail.isCanEdit();
        boolean canEditSrc = mMergeDetail.isCanEditSrcBranch();
        boolean isMyMerge = mMerge.authorIsMe();
        int granted = mMerge.getGranted();

        boolean showCancel = mMerge.authorIsMe();
        boolean showMerge = canEdit || (granted == 1 && mMerge.authorIsMe());
        boolean showRefuse = canEdit;
        boolean showAuth = canEdit && granted == 0 && !mMerge.authorIsMe() && !mMergeDetail.authorCanEdit();
        boolean showCancelAuth = canEdit && granted == 1 && !mMerge.authorIsMe() && !mMergeDetail.authorCanEdit();

        if (mMerge.isStyleCanMerge()) {
            setActionStyle(showMerge, showRefuse, showCancel, showAuth, showCancelAuth);
        } else if (mMerge.isStyleCannotMerge()) {
            setActionStyle(null, canEdit, canEditSrc, showAuth, showCancelAuth);
        } else {
            setActionStyle(false, false, false, false, false);
        }
    }

    /**
     * @param accept 这个参数可以为 null，代表不可合并，但是显示合并按钮，并 50% 透明
     */
    private void setActionStyle(Boolean accept, boolean refuse, boolean cancel, boolean auth, boolean cancelAuth) {
        if (!refuse && accept != null && !accept && !cancel && !auth && !cancelAuth) {
            actionLayout.setVisibility(View.GONE);
        } else {
            if (cancel) refuse = false; // 显示"取消"按钮的时候不显示"拒绝"

            if (accept != null) {
                actionAccept.setVisibility(accept ? View.VISIBLE : View.GONE);
                actionAccept.setAlpha(1f);
                actionAccept.setTag(null);
            } else {
                actionAccept.setVisibility(View.VISIBLE);
                actionAccept.setTag("Coding 不能帮你在线自动合并这个请求。");
                actionAccept.setAlpha(.5f);
            }
            actionLayout.setVisibility(View.VISIBLE);
            actionRefuse.setVisibility(refuse ? View.VISIBLE : View.GONE);
            actionCancel.setVisibility(cancel ? View.VISIBLE : View.GONE);
            actionAuth.setVisibility(auth ? View.VISIBLE : View.GONE);
            actionCancelAuth.setVisibility(cancelAuth ? View.VISIBLE : View.GONE);
        }
    }

    @Click
    protected final void actionAccept(View view) {
        if (view.getTag() == null) {
            MergeAcceptActivity_.intent(this).mMergeDetail(mMergeDetail).startForResult(RESULT_MERGE);
        } else {
            showDialog("提示", "Coding 不能帮你在线自动合并这个请求。", null, null, "知道了", null);
        }
    }

    @Click
    protected final void actionRefuse() {
        showDialog(mMerge.getTitle(), "确定要拒绝吗？", (dialog, which) -> {
            String host = mMerge.getHttpRefuse();
            postNetwork(host, HOST_MERGE_REFUSE);
        });
    }

    @Click
    protected final void actionCancel() {
        showDialog(mMerge.getTitle(), "确定要取消吗？", (dialog, which) -> {
            String host = mMerge.getHttpCancel();
            postNetwork(host, HOST_MERGE_CANNEL);
        });
    }

    @Click
    protected final void actionAuth() {
        showDialog(mMerge.getTitle(), "确定要授权吗？", (dialog, which) -> {
            String host = mMerge.getHttpGrant();
//                postNetwork(host, HOST_MERGE_CANNEL);
            MyAsyncHttpClient.post(MergeDetailActivity.this, host, new MyJsonResponse(MergeDetailActivity.this) {
                @Override
                public void onMySuccess(JSONObject response) {
                    setResult(RESULT_OK);
                    mMerge.setGranted(1);
                    actionAuth.setVisibility(View.GONE);
                    actionCancelAuth.setVisibility(View.VISIBLE);
                }
            });

        });
    }

    @Click
    protected final void actionCancelAuth() {
        showDialog(mMerge.getTitle(), "确定要撤消授权吗？", (dialog, which) -> {
            String host = mMerge.getHttpGrant();
//                postNetwork(host, HOST_MERGE_CANNEL);
            MyAsyncHttpClient.delete(MergeDetailActivity.this, host, new MyJsonResponse(MergeDetailActivity.this) {
                @Override
                public void onMySuccess(JSONObject response) {
                    setResult(RESULT_OK);
                    mMerge.setGranted(0);
                    actionAuth.setVisibility(View.VISIBLE);
                    actionCancelAuth.setVisibility(View.GONE);
                }
            });
        });
    }

    private void initHead(View head) {
        head.findViewById(R.id.itemCommit).setOnClickListener(v ->
                CommitListActivity_.intent(MergeDetailActivity.this).mMerge(mMerge).start());

        ListItem1 itemFiles = (ListItem1) head.findViewById(R.id.itemFile);
        itemFiles.showBadge(RedPointTip.show(this, RedPointTip.Type.MergeFile320));
        itemFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RedPointTip.markUsed(MergeDetailActivity.this, RedPointTip.Type.MergeFile320);
                ((ListItem1) v).showBadge(false);
                MergeFileListActivity_.intent(MergeDetailActivity.this).mMerge(mMerge).start();
            }
        });

        ((TextView) head.findViewById(R.id.title)).setText(mMerge.getTitleSpannable());

        TextView userText = head.findViewById(R.id.userName);
        userText.setText(mMerge.getAuthor().name);

        ((TextView) head.findViewById(R.id.time)).setText(Global.dayToNowCreate(mMerge.getCreatedAt()));

        TextView styleView = head.findViewById(R.id.mergeStyle);

        Merge.Status status = mMerge.getMergeStatus();
        styleView.setText(status.alics);
        styleView.setTextColor(status.color);

        String src = mMerge.getSrcBranch();
        String desc = mMerge.getDescBranch();

        ((TextView) head.findViewById(R.id.branchSrc)).setText(src);
        ((TextView) head.findViewById(R.id.branchDesc)).setText(desc);


        ((TextView) head.findViewById(R.id.mergeActionUser)).setText(mMerge.getActionAuthor().name);
        String mergeActionMessage = String.format(" %s %s这个%s", Global.dayToNow(mMerge.getAction_at()),
                mMerge.getMergeStatus().alics, ProjectObject.getTitle(mMerge.isPull()));
        ((TextView) head.findViewById(R.id.mergeLog)).setText(mergeActionMessage);


        View mergeTreate = head.findViewById(R.id.mergeTreate);
        if (mMerge.isMergeTreate()) {
            mergeTreate.setVisibility(View.VISIBLE);
            int color;
            int iconRes;
            if (mMerge.isMergeAccept()) {
                color = R.color.font_green;
                iconRes = R.drawable.ic_listitem_merge_accept;
            } else {
                color = R.color.font_red;
                iconRes = R.drawable.ic_listitem_merge_refuse;
            }
            head.findViewById(R.id.mergeIcon0).setBackgroundResource(color);
            head.findViewById(R.id.mergeIcon1).setBackgroundResource(iconRes);
        } else {
            mergeTreate.setVisibility(View.GONE);
        }

        // 取到 detail 后再显示
        contentWeb = (WebView) head.findViewById(R.id.comment);
        contentWeb.setVisibility(View.GONE);

        head.findViewById(R.id.itemRefResource).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskParam param = new TaskParam(mMerge.getProjectPath(),
                        mMerge.getIid());

                RefResourceActivity_.intent(MergeDetailActivity.this)
                        .mData(refResourceList)
                        .mParam(param)
                        .startForResult(RESULT_RESUSE_REFRESOURCE);
            }
        });

    }

    private void initFooter(View footer) {
        footer.findViewById(R.id.itemAddComment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommentActivity.CommentParam param = createParam("");
                CommentActivity_.intent(MergeDetailActivity.this).mParam(param).startForResult(RESULT_COMMENT);
            }
        });
    }

    public CommentActivity.CommentParam createParam(final String atSomeOne) {
        return new MergeCommentParam(mMerge, atSomeOne);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_COMMENT) {
            if (resultCode == RESULT_OK) {
                String commentString = (String) data.getSerializableExtra("data");
                try {
                    JSONObject json = new JSONObject(commentString);
                    DynamicObject.DynamicMergeRequest comment = new DynamicObject.DynamicMergeRequest(json, true);
                    mAdapter.appendData(comment);
                    mAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    showButtomToast("" + e.toString());
                }
            }

        } else if (requestCode == RESULT_MERGE) {
            if (resultCode == RESULT_OK) {
                finishAndUpdateList();
            }
        }
    }

    @Override
    public void parseJson(int code, JSONObject respanse, String tag, int pos, Object data) throws JSONException {
        if (tag.equals(HOST_DELETE_COMMENT)) {
            if (code == 0) {
                mAdapter.removeDataUpdate((DynamicObject.DynamicMergeRequest) data);
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(HOST_MERGE_REFUSE)) {
            umengEvent(UmengEvent.CODE, "拒绝mrpr");
            umengEvent(UmengEvent.E_GIT, "MR拒绝");
            if (code == 0) {
                finishAndUpdateList();
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(HOST_MERGE_CANNEL)) {
            umengEvent(UmengEvent.CODE, "取消mrpr");
            if (code == 0) {
                finishAndUpdateList();
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(HOST_MERGE_DETAIL)) {
            hideProgressDialog();
            if (code == 0) {
                mMergeDetail = new MergeDetail(respanse.optJSONObject("data"));

                if (mMerge == null) {
                    mMerge = mMergeDetail.getMerge();
                    initByMereData();
                } else {
                    mMerge = mMergeDetail.getMerge();
                }

                updateBottomBarStyle();
                if (!TextUtils.isEmpty(mMergeDetail.getBody())) {
                    contentWeb.setVisibility(View.VISIBLE);
                    CodingGlobal.setWebViewContent(contentWeb, "topic-android.html", mMergeDetail.getBody());
                }

                refreshReviewers();
                refreshRefResource();
            } else {
                showErrorMsg(code, respanse);

                if (code == NetworkImpl.ERROR_PERMISSION_DENIED) {
                    BlankViewDisplay.setBlank(0, this, true, blankLayout, null, "无权访问\n请联系项目管理员进行代码权限设置");
                } else {
                    BlankViewDisplay.setBlank(0, this, true, blankLayout, null);
                }
            }
        } else if (tag.equals(TAG_REVIEW_GOOD)) {
            if (code == 0) {
                refreshReviewers();
            }
        }
    }

    private void refreshRefResource() {
        if (!mMerge.isPull()) {
            String url = Global.HOST_API + mMerge.getProjectPath() + "/resource_reference/" + mMerge.getIid();
            MyAsyncHttpClient.get(this, url, new MyJsonResponse(this) {
                @Override
                public void onMySuccess(JSONObject response) {
                    super.onMySuccess(response);

                    refResourceList.clear();
                    JSONObject jsonData = response.optJSONObject("data");
                    Iterator<String> iter = jsonData.keys();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        JSONArray array = jsonData.optJSONArray(key);
                        for (int i = 0; i < array.length(); ++i) {
                            JSONObject item = array.optJSONObject(i);
                            try {
                                refResourceList.add(new RefResourceObject(item));
                            } catch (Exception e) {
                                Global.errorLog(e);
                            }
                        }
                    }

                    updateRefResourceUI();
                }
            });
        }
    }

    private void updateRefResourceUI() {
        View item = findViewById(R.id.itemRefResource);
        if (refResourceList.isEmpty()) {
            item.setVisibility(View.GONE);
        } else {
            item.setVisibility(View.VISIBLE);

            ((TextView) item.findViewById(R.id.text2)).setText(refResourceList.size() + "个资源");
            item.findViewById(R.id.text2).setVisibility(View.VISIBLE);
        }
    }

    private void refreshReviewers() {
        if (!mMerge.isPull()) {
            MyAsyncHttpClient.get(this, mMerge.getHttpReviewers(), new MyJsonResponse(this) {
                @Override
                public void onMySuccess(JSONObject response) {
                    Log.d("reviewers", response.toString());
                    JSONArray json = null;
                    try {
                        json = response.optJSONObject("data").optJSONArray("reviewers");
                        ArrayList<Merge.Reviewer> arrayData = new ArrayList<>();
                        for (int i = 0; i < json.length(); ++i) {
                            Merge.Reviewer user = new Merge.Reviewer(json.getJSONObject(i));
                            arrayData.add(user);
                        }
                        json = response.optJSONObject("data").optJSONArray("volunteer_reviewers");
                        for (int i = 0; i < json.length(); ++i) {
                            Merge.Reviewer user = new Merge.Reviewer(json.getJSONObject(i));
                            arrayData.add(user);
                        }
                        reviewerList = arrayData;
                        updateReviewer();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        } else {
            updateReviewer();
        }
    }

    private void refreshActivities() {
        dynamicList.clear();
        String commentsUrl = mMerge.getHttpComments();
        String activitiesUrl = mMerge.getHttpActivities();

        MyJsonResponse activityResponse = new MyJsonResponse(this) {
            @Override
            public void onMySuccess(JSONObject response) {
                try {
                    JSONArray json = response.getJSONArray("data");
                    for (int i = 0; i < json.length(); ++i) {
                        Object object = json.get(i);
                        JSONObject activityJson = null;
                        boolean isCommentCommit = false;
                        if (object instanceof JSONArray) {  //  comment 的结构是  data : [ [comment] , [comment]]
                            activityJson = ((JSONArray) object).getJSONObject(0);
                            if (activityJson.has("diff_html")) {
                                isCommentCommit = true;
                                activityJson.put("action", "comment_commit");
                            } else {
                                activityJson.put("action", "comment");
                            }
                        } else if (object instanceof JSONObject) {  //  activity 的结构是  data : [ activity , activity]
                            activityJson = (JSONObject) object;
                        }

                        DynamicObject.DynamicMergeRequest activity =
                                isCommentCommit ?
                                        new DynamicObject.DynamicMergeRequestCommentCommit(activityJson) :
                                        new DynamicObject.DynamicMergeRequest(activityJson);
                        dynamicList.add(activity);

                        Collections.sort(dynamicList, mDynamicSorter);
                    }
                    if (dynamicList.size() > 0) {
                        listView.findViewById(R.id.gap_to_list).setVisibility(View.VISIBLE);
                    } else {
                        listView.findViewById(R.id.gap_to_list).setVisibility(View.GONE);
                    }
                    mAdapter.resetData(dynamicList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        };
        MyAsyncHttpClient.get(this, activitiesUrl, activityResponse);
        MyAsyncHttpClient.get(this, commentsUrl, activityResponse);

    }

    private void finishAndUpdateList() {
        setResult(RESULT_OK);

        EventBus.getDefault().post(MergeListFragment.EVENT_UPDATE);

        finish();
    }

    private void updateReviewer() {
        if (mMerge == null || mMerge.isPull()) {
            findViewById(R.id.reviewer_layout).setVisibility(View.GONE);
            return;
        } else {
            findViewById(R.id.reviewer_layout).setVisibility(View.VISIBLE);
        }
        ListItem1 reviewers = (ListItem1) findViewById(R.id.itemReviewer);
        int role = 0;
        if (mMerge.isMergeTreate() || mMerge.isCanceled()) {
            role = 0;
        } else if (mMerge.authorIsMe()) {
            role = 1;
        } else {
            role = 2;
            if (reviewerList != null) {
                for (Merge.Reviewer reviewer : reviewerList) {
                    if (GlobalData.sUserObject.id == reviewer.user.id) {
                        if (reviewer.value > 0)
                            role = 3;
                        break;
                    }
                }
            }
        }
        if (role > 0) {
            TextView tv = (TextView) reviewers.findViewById(R.id.text2);
            tv.setVisibility(View.VISIBLE);
            View arrow = reviewers.findViewById(R.id.arrow);
            if (role == 1) {
                tv.setText("添加");
                tv.setTextColor(getResources().getColor(R.color.font_3));
                tv.setCompoundDrawables(null, null, null, null);
                arrow.setVisibility(View.VISIBLE);
            } else if (role == 3) {
                tv.setText("撤消 +1 ");
                tv.setTextColor(getResources().getColor(R.color.font_green));
                tv.setCompoundDrawables(null, null, null, null);
                arrow.setVisibility(View.GONE);
            } else if (role == 2) {
                tv.setText("+1  ");
                Drawable up = getResources().getDrawable(R.drawable.thumb_up);
                up.setBounds(0, 0, up.getMinimumWidth(), up.getMinimumHeight());
                tv.setCompoundDrawables(up, null, null, null);
                arrow.setVisibility(View.GONE);
                tv.setTextColor(getResources().getColor(R.color.font_green));
            }
            tv.setGravity(Gravity.CENTER);
        } else {
            TextView tv = (TextView) reviewers.findViewById(R.id.text2);
            tv.setVisibility(View.GONE);
            View arrow = reviewers.findViewById(R.id.arrow);
            if (arrow != null) {
                arrow.setVisibility(View.VISIBLE);
            }
        }
        final int roleFinal = role;
        reviewers.findViewById(R.id.text2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (roleFinal == 0) {
                    Intent intent = new Intent(MergeDetailActivity.this, MembersSelectActivity_.class);
                    intent.putExtra("mMerge", mMerge);
                    startActivityForResult(intent, MergeReviewerListFragment.RESULT_ADD_USER);
                } else if (roleFinal == 1) {
                    Intent intent = new Intent(MergeDetailActivity.this, MembersSelectActivity_.class);
                    intent.putExtra("mMerge", mMerge);
                    intent.putExtra("mSelect", true);
                    startActivityForResult(intent, MergeReviewerListFragment.RESULT_ADD_USER);
                } else if (roleFinal == 2) {
                    postNetwork(mMerge.getHttpReviewGood(), new RequestParams(), TAG_REVIEW_GOOD);
                } else if (roleFinal == 3) {
                    deleteNetwork(mMerge.getHttpReviewGood(), TAG_REVIEW_GOOD);
                }
            }
        });


        LinearLayout reviewersLayout = findViewById(R.id.reviewers);
        reviewersLayout.removeAllViews();
        if (reviewerList != null && reviewerList.size() > 0) {
            int imageSize = DensityUtil.dip2px(this, 38);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(imageSize, imageSize);
            lp.rightMargin = DensityUtil.dip2px(this, 8);
            int addedCount = 0;
            int shouldShowCount = getResources().getInteger(R.integer.max_reviewer_count);
            boolean shouldShowMore = false;
            if (reviewerList.size() > shouldShowCount) {
                shouldShowMore = true;
                shouldShowCount = shouldShowCount - 1;
            } else {
                shouldShowCount = reviewerList.size();
            }
            for (int i = 0; i < shouldShowCount; i++) {
                Merge.Reviewer reviewer = reviewerList.get(i);
                if (reviewer.user.global_key.equals(mMerge.getAuthor().global_key)) {
                    continue;
                }

                CircleImageView circleImageView = new CircleImageView(this);
                circleImageView.setOnClickListener(GlobalCommon.mOnClickUser);
                circleImageView.setTag(reviewer.user.global_key);

                if ("invitee".equals(reviewer.volunteer) && reviewer.value > 0) {
                    FrameLayout container = new FrameLayout(this);
                    container.addView(circleImageView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    ImageView thumb = new ImageView(this);
                    thumb.setImageResource(R.drawable.thumb_uped);
                    container.addView(thumb, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.RIGHT));
                    reviewersLayout.addView(container, lp);
                } else {
                    reviewersLayout.addView(circleImageView, lp);
                }
                addedCount++;
                iconfromNetwork(circleImageView, reviewer.user.avatar);
            }

            ImageView more = new ImageView(this);
            reviewersLayout.addView(more, lp);

            if (shouldShowMore) {
                more.setBackgroundResource(R.drawable.round_more);
            } else {
                more.setBackgroundResource(R.drawable.merge_request_create);
            }

            if (addedCount > 0) {
                findViewById(R.id.reviewer_divide).setVisibility(View.VISIBLE);
                reviewersLayout.setVisibility(View.VISIBLE);
                reviewersLayout.setOnClickListener(clickJumpReviewUsers);
            } else {
                findViewById(R.id.reviewer_divide).setVisibility(View.GONE);
                reviewersLayout.setVisibility(View.GONE);
            }

        } else {
            findViewById(R.id.reviewer_divide).setVisibility(View.GONE);
            reviewersLayout.setVisibility(View.GONE);
        }
    }

    @OnActivityResult(RESULT_RESUSE_REFRESOURCE)
    void resultRefResource(int resultCode, @OnActivityResult.Extra ArrayList<RefResourceObject> resultData) {
        if (resultCode == RESULT_OK) {
            refResourceList = resultData;
            updateRefResourceUI();
        }
    }

    @OnActivityResult(MergeReviewerListFragment.RESULT_ADD_USER)
    public void onAddReviewer(int result) {
        if (result == Activity.RESULT_OK) {
            refreshReviewers();
        }
    }

    public static class MergeCommentParam extends CommentActivity.CommentParam implements Serializable {
        private Merge mMerge;
        private String atSomeOne;

        public MergeCommentParam(Merge mMerge, String atSomeOne) {
            this.mMerge = mMerge;
            this.atSomeOne = atSomeOne;
        }

        @Override
        public RequestData getSendCommentParam(String input) {
            RequestData request = mMerge.getHttpSendComment();
            request.setContent(input);
            return request;
        }

        @Override
        public String getAtSome() {
            return atSomeOne;
        }

        @Override
        public String getAtSomeUrl() {
            return mMerge.getMergeAtMemberUrl();
        }

        @Override
        public String getProjectPath() {
            return mMerge.getProjectPath();
        }

        @Override
        public boolean isPublicProject() {
            return mMerge.isPull();
        }
    }

    class DynamicHolder {
        public ImageView mIcon;
        public TextView mContent;
        public TextView mContent2;
        private View timeLineUp;
        private View timeLineDown;

        public DynamicHolder(View convertView) {
            mIcon = (ImageView) convertView.findViewById(R.id.icon);
            mContent = (TextView) convertView.findViewById(R.id.content);
            mContent2 = (TextView) convertView.findViewById(R.id.linkContent);
            mContent.setMovementMethod(LongClickLinkMovementMethod.getInstance());
            timeLineUp = convertView.findViewById(R.id.timeLineUp);
            timeLineDown = convertView.findViewById(R.id.timeLineDown);
        }

        public void updateLine(int position, int count) {
            switch (count) {
                case 1:
                    setLine(false, false);
                    break;

                default:
                    if (position == 0) {
                        setLine(false, true);
                    } else if (position == count - 1) {
                        setLine(true, false);
                    } else {
                        setLine(true, true);
                    }
                    break;
            }
        }

        private void setLine(boolean up, boolean down) {
            timeLineUp.setVisibility(up ? View.VISIBLE : View.INVISIBLE);
            timeLineDown.setVisibility(down ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
