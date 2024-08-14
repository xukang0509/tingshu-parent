package com.atguigu.tingshu.dispatch.job.common;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.tingshu.model.dispatch.XxlJobLog;
import com.xxl.job.core.context.XxlJobHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * @author xk
 * @since 2024-08-13 20:56
 */
@Slf4j
@Aspect
@Component
public class TingShuHandlerAspect {

    @Resource
    private XxlJobLogMapper xxlJobLogMapper;

    @Around("@annotation(tingShuHandler)")
    public Object loginAspect(ProceedingJoinPoint joinPoint, TingShuHandler tingShuHandler) {
        String describe = tingShuHandler.describe();
        log.info("{}：{}", describe, XxlJobHelper.getJobId());
        long startTimes = System.currentTimeMillis();

        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        try {
            Result result = (Result) joinPoint.proceed();
            if (result == null) {
                // 如果result为空，则说明远程调用失败
                xxlJobLog.setStatus(0);
                xxlJobLog.setError("远程调用更新接口失败！");
                log.error("{}失败！任务id：{}", describe, xxlJobLog.getJobId());
                // 任务处理失败
                XxlJobHelper.handleFail();
            } else {
                // 远程调用成功
                xxlJobLog.setStatus(1);
                XxlJobHelper.handleSuccess();
            }
        } catch (Throwable e) {
            // 发生异常，任务执行失败
            xxlJobLog.setStatus(0);
            xxlJobLog.setError("任务执行失败！");
            log.error("{}！任务id：{}", describe, xxlJobLog.getJobId());
            XxlJobHelper.handleFail();
        } finally {
            // 处理执行时长
            xxlJobLog.setTimes((int) (System.currentTimeMillis() - startTimes));
            this.xxlJobLogMapper.insert(xxlJobLog);
        }
        return null;
    }
}
