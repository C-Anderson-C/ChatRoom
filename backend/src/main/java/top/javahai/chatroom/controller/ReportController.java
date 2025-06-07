package top.javahai.chatroom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import top.javahai.chatroom.entity.Report;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.service.ReportService;

@RestController
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping("/report/submit")
    public RespBean submitReport(@RequestBody Report report) {
        try {
            System.out.println("接收到举报请求: " + report);

            // 数据验证
            if (report.getUserId() == null) {
                return RespBean.error("举报人ID不能为空");
            }

            if (report.getContent() == null || report.getContent().trim().isEmpty()) {
                return RespBean.error("举报内容不能为空");
            }

            Integer result = reportService.addReport(report);
            if (result > 0) {
                return RespBean.ok("举报已提交，我们会尽快处理");
            }
            return RespBean.error("举报提交失败，请稍后再试");
        } catch (Exception e) {
            e.printStackTrace();
            return RespBean.error("服务器错误: " + e.getMessage());
        }
    }
}