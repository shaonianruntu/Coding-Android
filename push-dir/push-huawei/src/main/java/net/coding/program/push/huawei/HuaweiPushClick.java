package net.coding.program.push.huawei;

import android.content.Context;

import java.util.Map;

/**
 * Created by chenchao on 2017/11/9.
 */

// 测试数据
//    1984
//   coding://coding.net/push/huawei?param_url=https%3a%2f%2fcoding.net%2fu%2f1984%2fp%2fauth%2ftask%2f1732723&notification_id=11
public interface HuaweiPushClick {
    void click(Context context, Map<String, String> params);
}
