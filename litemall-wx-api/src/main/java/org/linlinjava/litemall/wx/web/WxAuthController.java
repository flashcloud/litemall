package org.linlinjava.litemall.wx.web;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.notify.NotifyService;
import org.linlinjava.litemall.core.notify.NotifyType;
import org.linlinjava.litemall.core.util.CharUtil;
import org.linlinjava.litemall.core.util.JacksonUtil;
import org.linlinjava.litemall.core.util.RegexUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.core.util.bcrypt.BCryptPasswordEncoder;
import org.linlinjava.litemall.db.domain.LitemallUser;
import org.linlinjava.litemall.db.domain.TraderOrderGoodsVo;
import org.linlinjava.litemall.db.service.CouponAssignService;
import org.linlinjava.litemall.db.service.LitemallOrderService;
import org.linlinjava.litemall.db.service.LitemallTraderService;
import org.linlinjava.litemall.db.service.LitemallUserService;
import org.linlinjava.litemall.wx.annotation.LoginUser;
import org.linlinjava.litemall.wx.dto.UserInfo;
import org.linlinjava.litemall.wx.dto.WxLoginInfo;
import org.linlinjava.litemall.wx.service.CaptchaCodeManager;
import org.linlinjava.litemall.wx.service.UserTokenManager;
import org.linlinjava.litemall.wx.service.WxOrderService;
import org.linlinjava.litemall.core.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.linlinjava.litemall.wx.util.WxResponseCode.*;

/**
 * 鉴权服务
 */
@RestController
@RequestMapping("/wx/auth")
@Validated
public class WxAuthController {
    private final Log logger = LogFactory.getLog(WxAuthController.class);

    @Autowired
    private Environment environment;    

    @Autowired
    private LitemallUserService userService;

    @Autowired
    private WxMaService wxService;

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private CouponAssignService couponAssignService;

    @Autowired LitemallTraderService traderService;

    @Autowired LitemallOrderService orderService;

    @Autowired private WxOrderService wxOrderService;

    /**
     * 账号登录
     *
     * @param body    请求内容，{ username: xxx, password: xxx }
     * @param request 请求对象
     * @return 登录结果
     */
    @PostMapping("login")
    public Object login(@RequestBody String body, HttpServletRequest request) {
        String username = JacksonUtil.parseString(body, "username");
        String password = JacksonUtil.parseString(body, "password");
        if (username == null) {
            username = JacksonUtil.parseString(body, "mobile");
        }
        if (username == null || password == null) {
            return ResponseUtil.badArgument();
        }

        List<LitemallUser> userList = userService.queryByUsername(username);
        LitemallUser user = null;
        if (userList.size() > 1) {
            return ResponseUtil.serious();
        } else if (userList.size() == 0) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "账号不存在");
        } else {
            user = userList.get(0);
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(password, user.getPassword())) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "账号密码不对");
        }

        // 更新登录情况
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(IpUtil.getIpAddr(request));
        if (userService.updateById(user) == 0) {
            return ResponseUtil.updatedDataFailed();
        }

        // userInfo
        UserInfo userInfo = new UserInfo();
        userInfo.setUserName(username);
        userInfo.setNickName(user.getNickname());
        userInfo.setAvatarUrl(user.getAvatar());
        userInfo.setGender(user.getGender());
        userInfo.setMobile(user.getMobile());
        userInfo.setAddTime(user.getAddTime());

        // 登录用户下面的所有交易商户
        userInfo.setManagedTraders(traderService.managedByUser(user));

        // token
        String token = UserTokenManager.generateToken(user.getId());

        Map<Object, Object> result = new HashMap<Object, Object>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return ResponseUtil.ok(result);
    }

    /**
     * 微信登录
     *
     * @param wxLoginInfo 请求内容，{ code: xxx, userInfo: xxx }
     * @param request     请求对象
     * @return 登录结果
     */
    @PostMapping("login_by_weixin")
    public Object loginByWeixin(@RequestBody WxLoginInfo wxLoginInfo, HttpServletRequest request) {
        String code = wxLoginInfo.getCode();
        UserInfo userInfo = wxLoginInfo.getUserInfo();
        if (code == null || userInfo == null) {
            return ResponseUtil.badArgument();
        }

        String sessionKey = null;
        String openId = null;
        try {
            WxMaJscode2SessionResult result = this.wxService.getUserService().getSessionInfo(code);
            sessionKey = result.getSessionKey();
            openId = result.getOpenid();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (sessionKey == null || openId == null) {
            return ResponseUtil.fail();
        }

        LitemallUser user = userService.queryByOid(openId);
        if (user == null) {
            user = new LitemallUser();
            user.setUsername(openId);
            user.setPassword(openId);
            user.setWeixinOpenid(openId);
            user.setAvatar(userInfo.getAvatarUrl());
            user.setNickname(userInfo.getNickName());
            user.setGender(userInfo.getGender());
            user.setUserLevel((byte) 0);
            user.setStatus((byte) 0);
            user.setLastLoginTime(LocalDateTime.now());
            user.setLastLoginIp(IpUtil.getIpAddr(request));
            user.setSessionKey(sessionKey);

            userService.add(user);

            // 新用户发送注册优惠券
            couponAssignService.assignForRegister(user.getId());
        } else {
            user.setLastLoginTime(LocalDateTime.now());
            user.setLastLoginIp(IpUtil.getIpAddr(request));
            user.setSessionKey(sessionKey);
            if (userService.updateById(user) == 0) {
                return ResponseUtil.updatedDataFailed();
            }
        }

        // token
        String token = UserTokenManager.generateToken(user.getId());

        Map<Object, Object> result = new HashMap<Object, Object>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return ResponseUtil.ok(result);
    }


    /**
     * 请求注册验证码
     *
     * TODO: 不用再使用短信验证码，使用图片验证码：https://github.com/xingyuv/captcha-plus
     * 这里需要一定机制防止短信验证码被滥用
     *
     * @param body 手机号码 { mobile }
     * @return
     */
    @PostMapping("regCaptcha")
    public Object registerCaptcha(@RequestBody String body) {
        String phoneNumber = JacksonUtil.parseString(body, "mobile");
        if (StringUtils.isEmpty(phoneNumber)) {
            return ResponseUtil.badArgument();
        }
        if (!RegexUtil.isMobileSimple(phoneNumber)) {
            return ResponseUtil.badArgumentValue();
        }

        if (!notifyService.isSmsEnable()) {
            return ResponseUtil.fail(AUTH_CAPTCHA_UNSUPPORT, "小程序后台验证码服务不支持");
        }
        String code = CharUtil.getRandomNum(6);
        boolean successful = CaptchaCodeManager.addToCache(phoneNumber, code);
        if (!successful) {
            return ResponseUtil.fail(AUTH_CAPTCHA_FREQUENCY, "验证码未超时1分钟，不能发送");
        }
        notifyService.notifySmsTemplate(phoneNumber, NotifyType.CAPTCHA, new String[]{code});

        return ResponseUtil.ok();
    }

    /**
     * 账号注册
     * TODO: 限制用户注册频率：https://blog.csdn.net/baiyan3212/article/details/90453765
     *      在注解上实际频率的动态设置：https://blog.csdn.net/qq_18244417/article/details/117678422， https://www.cnblogs.com/54chensongxia/p/15250479.html
     *
     * @param body    请求内容
     *                {
     *                username: xxx,
     *                password: xxx,
     *                mobile: xxx
     *                code: xxx
     *                }
     *                其中code是手机验证码，目前还不支持手机短信验证码
     * @param request 请求对象
     * @return 登录结果
     * 成功则
     * {
     * errno: 0,
     * errmsg: '成功',
     * data:
     * {
     * token: xxx,
     * tokenExpire: xxx,
     * userInfo: xxx
     * }
     * }
     * 失败则 { errno: XXX, errmsg: XXX }
     */
    @PostMapping("register")
    public Object register(@RequestBody String body, HttpServletRequest request) {
        String username = JacksonUtil.parseString(body, "username");
        String password = JacksonUtil.parseString(body, "password");
        String mobile = JacksonUtil.parseString(body, "mobile");
        String code = JacksonUtil.parseString(body, "code");
        String dogKey = JacksonUtil.parseString(body, "pcapp-key");
        // 如果是小程序注册，则必须非空
        // 其他情况，可以为空
        String wxCode = JacksonUtil.parseString(body, "wxCode");

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password) || StringUtils.isEmpty(mobile)
                || StringUtils.isEmpty(code)) {
            return ResponseUtil.badArgument();
        }

        List<LitemallUser> userList = userService.queryByUsername(username);
        if (userList.size() > 0) {
            return ResponseUtil.fail(AUTH_NAME_REGISTERED, "用户名已注册");
        }

        userList = userService.queryByMobile(mobile);
        if (userList.size() > 0) {
            return ResponseUtil.fail(AUTH_MOBILE_REGISTERED, "手机号已注册");
        }
        if (!RegexUtil.isMobileSimple(mobile)) {
            return ResponseUtil.fail(AUTH_INVALID_MOBILE, "手机号格式不正确");
        }
        //判断验证码是否正确
        String cacheCode = CaptchaCodeManager.getCachedCaptcha(mobile);
        if (cacheCode == null || cacheCode.isEmpty() || !cacheCode.equals(code)) {
            // return ResponseUtil.fail(AUTH_CAPTCHA_UNMATCH, "验证码错误"); //暂时不验证
        }

        String openId = "";
        // 非空，则是小程序注册
        // 继续验证openid
        if(!StringUtils.isEmpty(wxCode)) {
            try {
                WxMaJscode2SessionResult result = this.wxService.getUserService().getSessionInfo(wxCode);
                openId = result.getOpenid();
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseUtil.fail(AUTH_OPENID_UNACCESS, "openid 获取失败");
            }
            userList = userService.queryByOpenid(openId);
            if (userList.size() > 1) {
                return ResponseUtil.serious();
            }
            if (userList.size() == 1) {
                LitemallUser checkUser = userList.get(0);
                String checkUsername = checkUser.getUsername();
                String checkPassword = checkUser.getPassword();
                if (!checkUsername.equals(openId) || !checkPassword.equals(openId)) {
                    return ResponseUtil.fail(AUTH_OPENID_BINDED, "openid已绑定账号");
                }
            }
        }

        LitemallUser user = null;
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPassword = encoder.encode(password);
        user = new LitemallUser();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setMobile(mobile);
        user.setWeixinOpenid(openId);
        user.setAvatar("https://yanxuan.nosdn.127.net/80841d741d7fa3073e0ae27bf487339f.jpg?imageView&quality=90&thumbnail=64x64");
        user.setNickname(username);
        user.setGender((byte) 0);
        user.setUserLevel((byte) 0);
        user.setStatus((byte) 0);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(IpUtil.getIpAddr(request));
        if (dogKey != null && !dogKey.isEmpty()) {
            //绑定加密锁注册
            TraderOrderGoodsVo orderGoodsVo = orderService.getTraderOrderedPCAppBySerial(dogKey);
            if (orderGoodsVo == null) {
                return ResponseUtil.fail(AUTH_DOG_KEY_NOT_EXISTS, "KEY号（" + dogKey + "）不存在");
            }
            if (!traderService.checkRegisterUser(dogKey)) {
                //TODO: 向交易企业负责人发送消息，提示注册用户数已达上限，让其购买更多注册用户
                return ResponseUtil.fail(AUTH_REGISTER_USERS_COUNT_MAX, "注册用户数已达上限值：" + orderGoodsVo.getMaxRegisterUsersCount());
            }
            traderService.registerUser(user, dogKey);
        } else {
            userService.add(user);
        }

        // 给新用户发送注册优惠券
        couponAssignService.assignForRegister(user.getId());

        // userInfo
        UserInfo userInfo = new UserInfo();
        userInfo.setNickName(username);
        userInfo.setAvatarUrl(user.getAvatar());

        // token
        String token = UserTokenManager.generateToken(user.getId());

        Map<Object, Object> result = new HashMap<Object, Object>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return ResponseUtil.ok(result);
    }

    /**
     * 请求验证码
     *
     * TODO
     * 这里需要一定机制防止短信验证码被滥用
     *
     * @param body 手机号码 { mobile: xxx, type: xxx }
     * @return
     */
    @PostMapping("captcha")
    public Object captcha(@LoginUser Integer userId, @RequestBody String body) {
        if(userId == null){
            return ResponseUtil.unlogin();
        }
        String phoneNumber = JacksonUtil.parseString(body, "mobile");
        String captchaType = JacksonUtil.parseString(body, "type");
        if (StringUtils.isEmpty(phoneNumber)) {
            return ResponseUtil.badArgument();
        }
        if (!RegexUtil.isMobileSimple(phoneNumber)) {
            return ResponseUtil.badArgumentValue();
        }
        if (StringUtils.isEmpty(captchaType)) {
            return ResponseUtil.badArgument();
        }

        if (!notifyService.isSmsEnable()) {
            return ResponseUtil.fail(AUTH_CAPTCHA_UNSUPPORT, "小程序后台验证码服务不支持");
        }
        String code = CharUtil.getRandomNum(6);
        boolean successful = CaptchaCodeManager.addToCache(phoneNumber, code);
        if (!successful) {
            return ResponseUtil.fail(AUTH_CAPTCHA_FREQUENCY, "验证码未超时1分钟，不能发送");
        }
        notifyService.notifySmsTemplate(phoneNumber, NotifyType.CAPTCHA, new String[]{code});

        return ResponseUtil.ok();
    }

    /**
     * 账号密码重置
     *
     * @param body    请求内容
     *                {
     *                password: xxx,
     *                mobile: xxx
     *                code: xxx
     *                }
     *                其中code是手机验证码，目前还不支持手机短信验证码
     * @param request 请求对象
     * @return 登录结果
     * 成功则 { errno: 0, errmsg: '成功' }
     * 失败则 { errno: XXX, errmsg: XXX }
     */
    @PostMapping("reset")
    public Object reset(@LoginUser Integer userId, @RequestBody String body, HttpServletRequest request) {
        if(userId == null){
            return ResponseUtil.unlogin();
        }

        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "用户不存在");
        }

        String oldPassword = JacksonUtil.parseString(body, "oldPassword");
        String newPassword = JacksonUtil.parseString(body, "newPassword");
        String mobile = JacksonUtil.parseString(body, "mobile");
        String code = JacksonUtil.parseString(body, "code");

        if (mobile == null || code == null || newPassword == null || oldPassword == null) {
            return ResponseUtil.badArgument();
        }

        //TODO: 验证码
        //判断验证码是否正确
        //String cacheCode = CaptchaCodeManager.getCachedCaptcha(mobile);
        //if (cacheCode == null || cacheCode.isEmpty() || !cacheCode.equals(code))
        //    return ResponseUtil.fail(AUTH_CAPTCHA_UNMATCH, "验证码错误");

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(oldPassword, user.getPassword())) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "账号的原密码不对");
        }        

        List<LitemallUser> userList = userService.queryByMobile(mobile);
        if (userList.size() > 1) {
            return ResponseUtil.serious();
        } else if (userList.size() == 0) {
            return ResponseUtil.fail(AUTH_MOBILE_UNREGISTERED, "手机号未注册");
        } else {
            user = userList.get(0);
        }
        if (!user.getId().equals(userId)) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "用户账号不匹配");
        }

        if (!RegexUtil.isMobileSimple(mobile)) {
            return ResponseUtil.fail(AUTH_INVALID_MOBILE, "手机号格式不正确");
        }
        String encodedPassword = encoder.encode(newPassword);
        user.setPassword(encodedPassword);

        if (userService.updateById(user) == 0) {
            return ResponseUtil.updatedDataFailed();
        }

        return ResponseUtil.ok();
    }

    /**
     * 账号手机号码重置
     *
     * @param body    请求内容
     *                {
     *                password: xxx,
     *                mobile: xxx
     *                code: xxx
     *                }
     *                其中code是手机验证码，目前还不支持手机短信验证码
     * @param request 请求对象
     * @return 登录结果
     * 成功则 { errno: 0, errmsg: '成功' }
     * 失败则 { errno: XXX, errmsg: XXX }
     */
    @PostMapping("resetPhone")
    public Object resetPhone(@LoginUser Integer userId, @RequestBody String body, HttpServletRequest request) {
        if(userId == null){
            return ResponseUtil.unlogin();
        }
        String password = JacksonUtil.parseString(body, "password");
        String mobile = JacksonUtil.parseString(body, "mobile");
        String code = JacksonUtil.parseString(body, "code");

        if (mobile == null || code == null || password == null) {
            return ResponseUtil.badArgument();
        }

        //TODO: 验证码
        //判断验证码是否正确
        // String cacheCode = CaptchaCodeManager.getCachedCaptcha(mobile);
        // if (cacheCode == null || cacheCode.isEmpty() || !cacheCode.equals(code))
        //     return ResponseUtil.fail(AUTH_CAPTCHA_UNMATCH, "验证码错误");

        List<LitemallUser> userList = userService.queryByMobile(mobile);
        LitemallUser user = null;
        if (userList.size() > 0) {
            return ResponseUtil.fail(AUTH_MOBILE_REGISTERED, "手机号已注册");
        }

        userList = userService.queryByUsername(mobile);
        if (userList.size() > 0) {
            return ResponseUtil.fail(AUTH_NAME_REGISTERED, "手机号注册的用户名已存在");
        }

        user = userService.findById(userId);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(password, user.getPassword())) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "账号密码不对");
        }

        user.setMobile(mobile);
        user.setUsername(mobile); // 将用户名也设置为手机号
        if (userService.updateById(user) == 0) {
            return ResponseUtil.updatedDataFailed();
        }

        return ResponseUtil.ok();
    }

    /**
     * 账号信息更新
     *
     * @param body    请求内容
     *                {
     *                password: xxx,
     *                mobile: xxx
     *                code: xxx
     *                }
     *                其中code是手机验证码，目前还不支持手机短信验证码
     * @param request 请求对象
     * @return 登录结果
     * 成功则 { errno: 0, errmsg: '成功' }
     * 失败则 { errno: XXX, errmsg: XXX }
     */
    @PostMapping("profile")
    public Object profile(@LoginUser Integer userId, @RequestBody String body, HttpServletRequest request) {
        if(userId == null){
            return ResponseUtil.unlogin();
        }
        String avatar = JacksonUtil.parseString(body, "avatar");
        Byte gender = JacksonUtil.parseByte(body, "gender");
        String nickname = JacksonUtil.parseString(body, "nickname");

        LitemallUser user = userService.findById(userId);
        if(!StringUtils.isEmpty(avatar)){
            user.setAvatar(avatar);
        }
        if(gender != null){
            user.setGender(gender);
        }
        if(!StringUtils.isEmpty(nickname)){
            user.setNickname(nickname);
        }

        if (userService.updateById(user) == 0) {
            return ResponseUtil.updatedDataFailed();
        }

        return ResponseUtil.ok();
    }

    @PostMapping("isLoginTimeout")
    public Object isLoginTimeout(@LoginUser Integer userId) {
        if(userId == null){
            return ResponseUtil.unlogin();
        } else {
            return ResponseUtil.ok();
        }
    }

    // 图片上传
    @PostMapping(value = "uploadAvatar", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public Object upload(@LoginUser Integer userId, @RequestParam("file") MultipartFile multipartFile) {
        if (userId == null) {
            return ResponseUtil.unlogin();
        }

        String avatarAbsPathStr = getAvatarAbsFolder();
        File avatarAbsFolder = new File(avatarAbsPathStr);
        if (!avatarAbsFolder.exists()) {
            avatarAbsFolder.mkdirs();
        }

        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "用户不存在");
        }

        //获取文件扩展名
        String originalFilename = multipartFile.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String avatarFileName = user.getUsername() + fileExtension;

        // 保存头像名称为用户账户名
        String avatarUrl = getAvatarFolder() +  avatarFileName;
        String avatarFileLocalPath = avatarAbsPathStr + avatarFileName;
		File avatarFile = new File(avatarFileLocalPath);
        

        // 在avatarAbsFolder文件夹下搜索所有以用户账户名开头的的其他文件，并删除
        File[] files = avatarAbsFolder.listFiles((dir, name) -> name.startsWith(user.getUsername()));
        if (files != null) {
            for (File f : files) {
                if (!f.equals(avatarFile)) {
                    f.delete();
                }
            }
        }

        try {
            multipartFile.transferTo(avatarFile);
            
            //上传头像到服务器本地后，清空用户的头像地址
            user.setAvatar(avatarUrl); // 清空头像地址，使用本地存储的头像
            if (userService.updateById(user) == 0) {
                return ResponseUtil.updatedDataFailed();
            }

            // 这里可以添加图片上传逻辑，比如上传到云存储等
            // 目前仅返回图片地址
            Map<Object, Object> data = new HashMap<>();
            data.put("avatar", avatarUrl);
            return ResponseUtil.ok(data);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseUtil.fail(502, "图片上传失败: " + e.getMessage());
        }
    }

    @GetMapping("getAvatar")
    public ResponseEntity<Resource> getAvatar( @NotNull String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(501).build(); // 未登录
        }
        Integer userId = UserTokenManager.getUserId(token);
        if (userId == null) {
            return ResponseEntity.status(501).build(); // 未登录
        }

        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(404).build(); // 用户不存在
        }

        Resource resource;

        if (!StringUtils.isEmpty(user.getAvatar()) && !user.getAvatar().startsWith(getAvatarFolder())) {
            //如果用户设置了头像的URL地址，则直接返回该头像
            return ResponseEntity.status(404).build();
        } else {
            // 获取头像文件夹路径
            String avatarAbsPathStr = getAvatarAbsFolder();
            File avatarAbsFolder = new File(avatarAbsPathStr);
            if (!avatarAbsFolder.exists()) {
                return ResponseEntity.status(201).build(); // 有头像的URL,则返回201让客户端自己加载
            }
            //在avatarAbsFolder文件夹下搜索所有以用户账户名开头的的文件，返回第一个找到的文件
            File[] files = avatarAbsFolder.listFiles((dir, name) -> name.startsWith(user.getUsername()));
            if (files != null && files.length > 0) {
                File avatarFile = files[0];
                // 加载图片资源
                resource = loadResource(avatarFile.getAbsolutePath());
            } else {
                // 如果用户没有设置头像，则返回默认头像
                File defaultAvatarFile = new File(avatarAbsFolder, "avatar.jpg");   //TODO: 需要从配置文件中application.yml获取默认头像路径web.default-avatar-local
                if (!defaultAvatarFile.exists()) {
                    return ResponseEntity.status(404).build(); // 默认头像文件不存在
                }
                resource = loadResource(defaultAvatarFile.getAbsolutePath());
            }
        }

        // 构建响应
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
    private Resource loadResource(String imagePath) {
        // 实现加载图片资源的逻辑，这里可以是文件系统、类路径、网络等不同位置
        // 这里的示例是加载文件系统中的图片
        return new FileSystemResource(imagePath);
    }

    /**
     * 微信手机号码绑定
     *
     * @param userId
     * @param body
     * @return
     */
    @PostMapping("bindPhone")
    public Object bindPhone(@LoginUser Integer userId, @RequestBody String body) {
    	if (userId == null) {
            return ResponseUtil.unlogin();
        }
    	LitemallUser user = userService.findById(userId);
        String encryptedData = JacksonUtil.parseString(body, "encryptedData");
        String iv = JacksonUtil.parseString(body, "iv");
        WxMaPhoneNumberInfo phoneNumberInfo = this.wxService.getUserService().getPhoneNoInfo(user.getSessionKey(), encryptedData, iv);
        String phone = phoneNumberInfo.getPhoneNumber();
        user.setMobile(phone);
        if (userService.updateById(user) == 0) {
            return ResponseUtil.updatedDataFailed();
        }
        return ResponseUtil.ok();
    }

    @PostMapping("logout")
    public Object logout(@LoginUser Integer userId) {
        if (userId == null) {
            return ResponseUtil.unlogin();
        }
        return ResponseUtil.ok();
    }

    @GetMapping("info")
    public Object info(@LoginUser Integer userId) {
        if (userId == null) {
            return ResponseUtil.unlogin();
        }

        LitemallUser user = userService.findById(userId);
        Map<Object, Object> data = new HashMap<Object, Object>();
        data.put("nickName", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("gender", user.getGender());
        data.put("mobile", user.getMobile());

        return ResponseUtil.ok(data);
    }

    private String getAvatarFolder() {
        return "/images/avatar/";
    }    

    private String getAvatarAbsFolder() {
        String defaultPath= environment.getProperty("web.upload-path");
        if (defaultPath == null || defaultPath.isEmpty()) {
            //TODO: 这里需要修改为实际的图片存储路径
            defaultPath = "/Users/flashcloudf/dev/mobile_dev/worksapce/sungole-icloud/grsoft/litemall/storage";
        }
        return defaultPath + getAvatarFolder();
    }
}
