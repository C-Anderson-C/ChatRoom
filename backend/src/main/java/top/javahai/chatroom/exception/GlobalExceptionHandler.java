package top.javahai.chatroom.exception;

import com.mysql.cj.jdbc.exceptions.SQLExceptionsMapping;
// 或者需要其他具体异常类时根据实际情况引入
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.javahai.chatroom.entity.RespBean;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * @author Hai
 * @date 2020/4/27 - 19:49
 * 功能：全局异常处理类
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /*
  处理SQLException异常
   */
  @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
  public String handleSQLConstraintViolation(SQLIntegrityConstraintViolationException e) {
    // Handle exception
    return "Integrity constraint violated!";
  }
}
