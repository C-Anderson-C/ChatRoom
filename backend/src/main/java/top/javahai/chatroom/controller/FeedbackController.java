package top.javahai.chatroom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import top.javahai.chatroom.entity.Feedback;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.service.FeedbackService;

@RestController
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @PostMapping("/mail/feedback")
    public RespBean submitFeedback(@RequestBody Feedback feedback) {
        System.out.println("接收到反馈请求: " + feedback);
        Integer result = feedbackService.addFeedback(feedback);
        if (result > 0) {
            return RespBean.ok("反馈提交成功！");
        }
        return RespBean.error("反馈提交失败，请稍后再试");
    }
}