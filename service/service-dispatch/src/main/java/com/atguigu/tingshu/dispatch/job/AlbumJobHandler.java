package com.atguigu.tingshu.dispatch.job;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.tingshu.model.dispatch.XxlJobLog;
import com.atguigu.tingshu.search.client.SearchFeignClient;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author xk
 * @since 2024-08-13 20:05
 */
@Slf4j
@Component
public class AlbumJobHandler {
    @Resource
    private SearchFeignClient searchFeignClient;

    @Resource
    private XxlJobLogMapper xxlJobLogMapper;

    @XxlJob("updateLatelyAlbumStatJob")
    public void updateLatelyAlbumStatJob() {
        log.info("更新专辑统计信息到es：{}", XxlJobHelper.getJobId());
        long startTimes = System.currentTimeMillis();

        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        try {
            Result result = this.searchFeignClient.updateLatelyAlbumStat();
            if (result == null) {
                // 如果result为空，则说明远程调用失败
                xxlJobLog.setStatus(0);
                xxlJobLog.setError("远程调用更新接口失败！");
                log.error("更新专辑统计到es失败！任务id：{}", xxlJobLog.getJobId());
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
            log.error("更新专辑统计到es失败！任务id：{}", xxlJobLog.getJobId());
            XxlJobHelper.handleFail();
        } finally {
            // 处理执行时长
            xxlJobLog.setTimes((int) (System.currentTimeMillis() - startTimes));
            this.xxlJobLogMapper.insert(xxlJobLog);
        }
    }
}
