package net.coding.program.setting;


import android.widget.ImageView;
import android.widget.TextView;

import net.coding.program.MyApp;
import net.coding.program.R;
import net.coding.program.common.Global;
import net.coding.program.common.ui.BaseFragment;
import net.coding.program.model.UserObject;
import net.coding.program.model.user.ServiceInfo;
import net.coding.program.project.detail.file.LocalProjectFileActivity_;
import net.coding.program.user.UserPointActivity_;
import net.coding.program.user.team.TeamListActivity_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.json.JSONException;
import org.json.JSONObject;

@EFragment(R.layout.fragment_main_setting)
@OptionsMenu(R.menu.main_setting)
public class MainSettingFragment extends BaseFragment {

    private static final String TAG_SERVICE_INFO = "TAG_SERVICE_INFO";

    @ViewById
    TextView userName, userGK, projectCount, teamCount;

    ServiceInfo serviceInfo;

    @ViewById
    ImageView userIcon;

    @AfterViews
    void initMainSettingFragment() {
        UserObject me = MyApp.sUserObject;
        userName.setText(me.name);
        userGK.setText(String.format("个性后缀：%s", me.global_key));
        iconfromNetwork(userIcon, me.avatar);

        bindData();
        loadUser();
    }

    private void bindData() {
        if (serviceInfo == null) {
            return;
        }

        projectCount.setText(String.valueOf(serviceInfo.publicProject + serviceInfo.privateProject));
        teamCount.setText(String.valueOf(serviceInfo.team));
    }

    void loadUser() {
        String url = Global.HOST_API + "/user/service_info";
        getNetwork(url, TAG_SERVICE_INFO);
    }

    @Override
    public void parseJson(int code, JSONObject respanse, String tag, int pos, Object data) throws JSONException {
        if (tag.equals(TAG_SERVICE_INFO)) {
            if (code == 0) {
                serviceInfo = new ServiceInfo(respanse.optJSONObject("data"));
                bindData();
            } else {
                showErrorMsg(code, respanse);
            }
        }
        super.parseJson(code, respanse, tag, pos, data);
    }
    
    @Click
    void projectLayout() {
        // TODO: 16/9/19  未实现
    }

    @Click
    void teamLayout() {
        TeamListActivity_.intent(this).start();
    }
    
    @Click
    void itemAccount() {
        UserPointActivity_.intent(this).start();
    }

    @Click
    void itemLocalFile() {
        LocalProjectFileActivity_.intent(this).start();
    }

    @Click
    void itemHelp() {
        final String url = "https://coding.net/help";
        HelpActivity_.intent(this).url(url).title("帮助与反馈").start();
    }

    @Click
    void itemSetting() {
        SettingActivity_.intent(this).start();
    }

    @Click
    void itemAbout() {
        AboutActivity_.intent(this).start();
    }

    @OptionsItem
    void actionAddFollow() {

    }
}