package cn.css0209.flea.user.api;

import cn.css0209.flea.reptile.Zhengfang;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import io.netty.handler.codec.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author blankyk
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class Api {
    private Zhengfang zf;
    /**
     * 获取验证码
     *
     * @return 图片
     * @throws IOException
     */
    @RequestMapping(value = "/captcha", produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
    public Flux<byte[]> captCha(String path) throws IOException {
        log.info("<==请求验证码,path:"+path);
        zf = new Zhengfang();
        zf.captCha(path);
        File file = FileUtil.file("cn/css0209/flea/user/img/captCha" + path + ".jpg");
        FileInputStream inputStream = new FileInputStream(file);
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes, 0, inputStream.available());
        log.info("==>返回图片:captCha{}.jpg",path);
        return Flux.just(bytes);
    }

    @PostMapping("/login")
    public Mono<Result> login(@RequestBody JSONObject loginInfo, WebSession session) {
        Result result = new Result();
        String username = loginInfo.getStr("username");
        String password = loginInfo.getStr("password");
        String captcha = loginInfo.getStr("captcha");
        log.info("<=={}登录,验证码:{}",username,captcha);
        try {
            result = zf.login(username, password, captcha);
            JSONObject loginJson = result.getItem();
            session.getAttributes().put("name", loginJson.get("name"));
            session.getAttributes().put("xh", username);
        } catch (StringIndexOutOfBoundsException e) {
            result.setResult("fail");
            result.setMsg("登录失败,教务系统又双叒叕崩了！！");
        } catch (NullPointerException e) {
            result.setResult("fail");
            result.setMsg("输入错误，确认账号密码验证码正确");
        }
        log.info("==>"+result.toString());
        return Mono.just(result);
    }

    @GetMapping("myInfo")
    public Mono<Result> myInfo(WebSession session) {
        Result result = new Result();
        String name = session.getAttribute("name");
        String xh = session.getAttribute("xh");
        log.info("<=={}({})获取信息",name,xh);
        try {
            JSONObject infoJson = zf.myInfo(name, xh);
            result.setResult("success");
            result.setMsg("获取信息成功");
            result.setItem(infoJson);
        } catch (IndexOutOfBoundsException e) {
            result.setResult("fail");
            result.setMsg("出现异常了");
        }
        log.info("==>{}",result.toString());
        return Mono.just(result);
    }

    @GetMapping("failedGrade")
    public Mono<Result> failedGrade(WebSession session) {
        Result result = new Result();
        //从session中获取名字和学号
        String xh = session.getAttribute("xh");
        String name = session.getAttribute("name");
        log.info("<=={}({})获取未通过成绩",name,xh);
        try {
            JSONObject failedGradeJson = zf.failedGrade(xh, name);
            result.setResult("success");
            result.setMsg("查询到未通过成绩");
            result.setItem(failedGradeJson);
        } catch (NullPointerException e) {
            result.setMsg("请登录");
            result.setResult("fail");
        } catch (IndexOutOfBoundsException e) {
            result.setResult("fail");
            result.setMsg("查询失败");
            e.printStackTrace();
        }
        log.info("==>{}",result.toString());
        return Mono.just(result);
    }

    /**
     * 查询成绩
     *
     * @param session        session
     * @param year           年份
     * @param semester       学期
     * @param courseNature   课程性质
     * @param btn            查询按钮
     *                       {
     *                       "year": "2018-2019",
     *                       "semster": "2",
     *                       "course_natrue": "",
     *                       "btn": "btn_zg"
     *                       }
     * @return 查询成绩
     */
    @GetMapping("selectGrade")
    public Mono<Result> selectGrade(WebSession session, String year, String semester, String courseNature, String btn) {
        String name = session.getAttribute("name");
        String xh = session.getAttribute("xh");
        log.info("<=={}({})查询成绩",name,xh);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("year", year);
        jsonObject.put("semester", semester);
        jsonObject.put("course_nature", courseNature);
        jsonObject.put("btn", btn);
        Result result = zf.grade(name, xh, jsonObject);
        log.info("==>{}",result.toString());
        return Mono.just(result);
    }

    @DeleteMapping("loginOut")
    public Mono<Result> loginOut(WebSession session) {
        Result result = new Result();
        String name = session.getAttribute("name");
        String xh = session.getAttribute("xh");
        log.info("<=={}({})注销", name,xh);
        try {
            session.invalidate();
            result.setResult("success");
            result.setMsg("登出");
        } catch (NullPointerException e) {
            result.setResult("fail");
            result.setMsg("error:" + e);
        }
        log.info("==>{}", result.toString());
        return Mono.just(result);
    }
}
