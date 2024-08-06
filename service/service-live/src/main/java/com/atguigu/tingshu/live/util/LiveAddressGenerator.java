package com.atguigu.tingshu.live.util;

import com.atguigu.tingshu.vo.live.TencentLiveAddressVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Data
@Component
@ConfigurationProperties(prefix = "live")
public class LiveAddressGenerator {
    // 域名管理中点击推流域名-->推流配置-->鉴权配置-->主KEY
    private String pushKey;
    // 云直播控制台配置的推流域名
    private String pushDomain;
    // 云直播控制台配置的拉流域名
    private String pullDomain;
    // 直播SDK --> License管理 --> 应用管理 --> 自己创建应用中的应用名称
    private String appName;

    /**
     * 直播名称 及 直播时长
     *
     * @param streamName
     * @param txTime
     * @return
     */
    public TencentLiveAddressVo getAddressUrl(String streamName, long txTime) {
        //过期时间
        String safeUrl = getSafeUrl(pushKey, streamName, txTime);

        // 推流最终地址
        String pushUrl = "webrtc://" + pushDomain + "/" + appName + "/" + streamName + "?" + safeUrl;
        // 播放地址
        String playUrl = "webrtc://" + pullDomain + "/" + appName + "/" + streamName + "?" + safeUrl;
        TencentLiveAddressVo tencentLiveAddressVo = new TencentLiveAddressVo();
        tencentLiveAddressVo.setPushWebRtcUrl(pushUrl);
        tencentLiveAddressVo.setPullWebRtcUrl(playUrl);
        return tencentLiveAddressVo;
    }

    private static final char[] DIGITS_LOWER =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /*
     * KEY+ streamName + txTime
     */
    private static String getSafeUrl(String key, String streamName, long txTime) {
        String input = new StringBuilder().
                append(key).
                append(streamName).
                append(Long.toHexString(txTime).toUpperCase()).toString();

        String txSecret = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            txSecret = byteArrayToHexString(
                    messageDigest.digest(input.getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return txSecret == null ? "" :
                new StringBuilder().
                        append("txSecret=").
                        append(txSecret).
                        append("&").
                        append("txTime=").
                        append(Long.toHexString(txTime).toUpperCase()).
                        toString();
    }

    private static String byteArrayToHexString(byte[] data) {
        char[] out = new char[data.length << 1];

        for (int i = 0, j = 0; i < data.length; i++) {
            out[j++] = DIGITS_LOWER[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS_LOWER[0x0F & data[i]];
        }
        return new String(out);
    }
}
