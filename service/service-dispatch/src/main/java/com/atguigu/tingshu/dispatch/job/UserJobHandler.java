package com.atguigu.tingshu.dispatch.job;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.dispatch.job.common.TingShuHandler;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author xk
 * @since 2024-08-13 18:29
 */
@Component
@Slf4j
public class UserJobHandler {
    @Resource
    private UserInfoFeignClient userInfoFeignClient;

    @TingShuHandler(describe = "更新过期vip用户状态")
    @XxlJob("updateExpiredVipStatusJob")
    public Result updateExpiredVipStatusJob() {
        return this.userInfoFeignClient.updateExpiredVipStatus();
        /*log.info("更新过期vip用户的状态：{}", XxlJobHelper.getJobId());
        long startTimes = System.currentTimeMillis();

        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        try {
            Result result = this.userInfoFeignClient.updateExpiredVipStatus();
            if (result == null) {
                // 如果result为空，则说明远程调用失败
                xxlJobLog.setStatus(0);
                xxlJobLog.setError("远程调用更新接口失败！");
                log.error("更新过期vip用户状态失败！任务id：{}", xxlJobLog.getJobId());
                // 任务处理失败
                XxlJobHelper.handleFail();
            } else {
                // 远程调用成功
                xxlJobLog.setStatus(1);
                XxlJobHelper.handleSuccess();
            }
        } catch (Exception e) {
            // 发生异常，任务执行失败
            xxlJobLog.setStatus(0);
            xxlJobLog.setError("任务执行失败！");
            log.error("更新过期vip用户状态失败！任务id：{}", xxlJobLog.getJobId());
            XxlJobHelper.handleFail();
        } finally {
            // 处理执行时长
            xxlJobLog.setTimes((int) (System.currentTimeMillis() - startTimes));
            this.xxlJobLogMapper.insert(xxlJobLog);
        }*/
    }
}
