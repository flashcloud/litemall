package org.linlinjava.litemall.wx.web;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.mp.api.WxMpService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.notify.NotifyService;
import org.linlinjava.litemall.core.notify.NotifyType;
import org.linlinjava.litemall.core.storage.StorageService;
import org.linlinjava.litemall.core.util.CharUtil;
import org.linlinjava.litemall.core.util.JacksonUtil;
import org.linlinjava.litemall.core.util.RegexUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.core.util.bcrypt.BCryptPasswordEncoder;
import org.linlinjava.litemall.db.domain.LitemallGoods;
import org.linlinjava.litemall.db.domain.LitemallOrderGoods;
import org.linlinjava.litemall.db.domain.LitemallTrader;
import org.linlinjava.litemall.db.domain.LitemallUser;
import org.linlinjava.litemall.db.domain.TraderOrderGoodsVo;
import org.linlinjava.litemall.db.exception.DataStatusException;
import org.linlinjava.litemall.db.exception.MemberOrderDataException;
import org.linlinjava.litemall.db.service.CouponAssignService;
import org.linlinjava.litemall.db.service.LitemallGoodsService;
import org.linlinjava.litemall.db.service.LitemallMemberService;
import org.linlinjava.litemall.db.service.LitemallOrderGoodsService;
import org.linlinjava.litemall.db.service.LitemallOrderService;
import org.linlinjava.litemall.db.service.LitemallTraderService;
import org.linlinjava.litemall.db.service.LitemallUserService;
import org.linlinjava.litemall.wx.annotation.LoginUser;
import org.linlinjava.litemall.wx.annotation.support.LoginUserHandlerMethodArgumentResolver;
import org.linlinjava.litemall.wx.dto.UserInfo;
import org.linlinjava.litemall.wx.dto.WxLoginInfo;
import org.linlinjava.litemall.wx.service.CaptchaCodeManager;
import org.linlinjava.litemall.wx.service.UserInfoService;
import org.linlinjava.litemall.wx.service.UserTokenManager;
import org.linlinjava.litemall.core.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private UserInfoService userInfoService;

    @Autowired
    private WxMaService wxService;

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private CouponAssignService couponAssignService;

    @Autowired LitemallTraderService traderService;

    @Autowired LitemallOrderService orderService;
    @Autowired private LitemallOrderGoodsService orderGoodsService;
    @Autowired private LitemallGoodsService goodsService;
    @Autowired private LitemallMemberService memberService;

    @Value("${litemall.wx.app-id}")
    private String appId;

    @Value("${litemall.wx.app-secret}")
    private String appSecret;

    @Value("${litemall.wx.redirect-uri}")
    private String redirectUri;

    @Autowired
    private WxMpService wxMpService;

    @Bean
    private RestTemplate restTemplate(){
        RestTemplate restTemplate = new RestTemplate();
        //解决nickname中文乱码的问题
        List<HttpMessageConverter<?>> httpMessageConverters = restTemplate.getMessageConverters();
        httpMessageConverters.stream().forEach(httpMessageConverter -> {
            if(httpMessageConverter instanceof StringHttpMessageConverter){
                StringHttpMessageConverter messageConverter = (StringHttpMessageConverter) httpMessageConverter;
                messageConverter.setDefaultCharset(Charset.forName("UTF-8"));
            }
        });
        return restTemplate;
    }

    @PostMapping("isLoginExpired")
    public Object isLoginExpired(@RequestBody String body, HttpServletRequest request) {
        String token = request.getHeader(LoginUserHandlerMethodArgumentResolver.LOGIN_TOKEN_KEY);
        if (StringUtils.isEmpty(token)) {
            return ResponseUtil.badArgument();
        }
        Integer userId = UserTokenManager.getUserId(token);
        if (userId == null) {
            return ResponseUtil.fail(AUTH_TOKEN_EXPIRED, "登录会话过期"); // 未登录
        }

        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "账号不存在");
        }

        return ResponseUtil.ok();
    }

    /**
     * 账号或token登录
     *
     * @param body    请求内容，{ username: xxx, password: xxx }
     * @param request 请求对象
     * @return 登录结果
     */
    @PostMapping("login")
    public Object login(@RequestBody String body, HttpServletRequest request) {
        String username = JacksonUtil.parseString(body, "username");
        String password = JacksonUtil.parseString(body, "password");
        String token = "";
        LitemallUser user = null;
        if (!StringUtils.isEmpty(password)) {
            // 如果 password 非空，则使用账号密码登录
            if (username == null) {
                username = JacksonUtil.parseString(body, "mobile");
            }
            if (username == null || password == null) {
                return ResponseUtil.badArgument();
            }

            List<LitemallUser> userList = userService.queryByUsername(username);
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
        } else {
            //如果 password 为空，则使用request的header传入的 token 登录
            token = request.getHeader(LoginUserHandlerMethodArgumentResolver.LOGIN_TOKEN_KEY);
            if (token == null) {
                return ResponseEntity.status(501).build(); // 未登录
            }
            Integer userId = UserTokenManager.getUserId(token);
            if (userId == null) {
                return ResponseUtil.fail(AUTH_TOKEN_EXPIRED, "登录会话过期"); // 未登录
            }

            user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(404).build(); // 用户不存在
            }
        }

        // 更新登录情况
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(IpUtil.getIpAddr(request));
        if (userService.updateById(user) == 0) {
            return ResponseUtil.updatedDataFailed();
        }

        try {
            memberService.checkMemberStatus(user);
        } catch (IllegalArgumentException | MemberOrderDataException | DataStatusException e) {
            return ResponseUtil.fail(AUTH_USERS_MEMBER_STATUS, e.getMessage());
        } // 检查会员订单状态

        // userInfo
        UserInfo userInfo = initUserInfo(user, username);

        // token
        if (StringUtils.isEmpty(token)) {
            token = UserTokenManager.generateToken(user.getId());
        }

        Map<Object, Object> result = new HashMap<Object, Object>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return ResponseUtil.ok(result);
    }

    // 第一步：引导用户到微信授权页面获取code
    //@CrossOrigin(origins = "https://www.baidu.com")
    @GetMapping("/get_wechat_code")
    // @CrossOrigin(origins = "*")
    public Object getWeChatCode(@RequestParam String router, HttpServletResponse response) throws IOException {
        //实际获取code的地址：https://open.weixin.qq.com/connect/oauth2/authorize
        String url = wxMpService.getOAuth2Service().buildAuthorizationUrl(redirectUri + router, WxConsts.OAuth2Scope.SNSAPI_USERINFO, "STATE");
        //response.sendRedirect(url); //这里不能重定向，否则跨域了，由前端直接加载URL
        return ResponseUtil.ok(url);
    }

    /**
     * 注成功或登录成功后，初始化用户信息
     * @param user
     * @param username
     * @return
     */
    private UserInfo initUserInfo(LitemallUser user, String username) {
        //获取用户的会员类型的key值
        user.setMemberTypeKey(memberService.getUserMemberType(user));
        // userInfo
        UserInfo userInfo = UserInfo.cloneFromUser(user);
        userInfo.setUserName(username);

        // 登录用户下面的所有交易商户
        userInfo.setManagedTraders(traderService.managedByUser(user));

        // 登录用户相关的所有商户
        userInfo.setTraders(userInfoService.getTraders(user.getId()));
        return userInfo;
    }

    private Object getWechatAccessTokenAndOpenId(String code, String grantType) {
        
        String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?" +
                "appid=" + appId +
                "&secret=" + appSecret +
                "&code=" + code +
                "&grant_type=" + grantType;
        // 使用RestTemplate或HttpClient获取结果
        ResponseEntity<String> openIdResponse = restTemplate().getForEntity(tokenUrl, String.class);
        String errcode = JacksonUtil.parseString(openIdResponse.getBody(), "errcode");
        if (errcode != null) {
            return ResponseUtil.fail(Integer.parseInt(errcode), openIdResponse.getBody());
        }
        String accessToken = JacksonUtil.parseString(openIdResponse.getBody(), "access_token");
        String openId = JacksonUtil.parseString(openIdResponse.getBody(), "openid");
        List<String> result = new java.util.ArrayList<String>();
        result.add(accessToken);
        result.add(openId);
        return result;
    }

    // 第二步：微信回调处理
    private Object  getWechatUserInfo(String body) throws IOException {
        String code = JacksonUtil.parseString(body, "code");
        String state = JacksonUtil.parseString(body, "state");

        // 1. 使用code获取access_token和openid
        String accessToken = null;
        String openId = null;
        Object obj = getWechatAccessTokenAndOpenId(code, "authorization_code");
        if (obj instanceof List) {
            List<String> result = (List<String>) obj;
            accessToken = result.get(0);
            openId = result.get(1);
        } else {
            return obj;
        }
        
        // 2. 获取用户信息（如果需要）
        String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo?" +
                "access_token=" + accessToken +
                "&openid=" + openId +
                "&lang=zh_CN";
        ResponseEntity<String> userInfoResponse = restTemplate().getForEntity(userInfoUrl, String.class);

        // 3. 处理用户登录逻辑（创建或更新用户）
        UserInfo userInfo = new UserInfo();
        userInfo.setAccessToken(accessToken);
        userInfo.setWxOpenId(openId);
        userInfo.setNickName(JacksonUtil.parseString(userInfoResponse.getBody(), "nickname"));
        userInfo.setAvatarUrl(JacksonUtil.parseString(userInfoResponse.getBody(), "headimgurl"));
        userInfo.setGender(JacksonUtil.parseByte(userInfoResponse.getBody(), "sex"));
        userInfo.setCity(JacksonUtil.parseString(userInfoResponse.getBody(), "city"));
        userInfo.setProvince(JacksonUtil.parseString(userInfoResponse.getBody(), "province"));
        userInfo.setCountry(JacksonUtil.parseString(userInfoResponse.getBody(), "country"));
        userInfo.setLanguage(JacksonUtil.parseString(userInfoResponse.getBody(), "language"));
        WxLoginInfo wxLoginInfo = new WxLoginInfo();
        wxLoginInfo.setCode(code);
        wxLoginInfo.setUserInfo(userInfo);
        
        return wxLoginInfo;
    }

    /**
     * 在微信浏览器内登录
     * @param body
     * @param request
     * @return
     * @throws IOException
     */
    @PostMapping("login_by_weixin_h5")
    public Object loginByWeixinH5(@RequestBody String body, HttpServletRequest request) throws IOException {
        WxLoginInfo wxLoginInfo = null;
        Object objWxLoginInfo = getWechatUserInfo(body);
        if (objWxLoginInfo instanceof WxLoginInfo) {
            wxLoginInfo = (WxLoginInfo) objWxLoginInfo;
        } else {
            return objWxLoginInfo; // 失败返回错误信息
        }

        return loginByWeixinHelp(wxLoginInfo, request, true);
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
        return loginByWeixinHelp(wxLoginInfo, request, false);
    }
    private Object loginByWeixinHelp(WxLoginInfo wxLoginInfo, HttpServletRequest request, boolean isLoginByH5) {
        String code = wxLoginInfo.getCode();
        UserInfo userInfo = wxLoginInfo.getUserInfo();
        if (code == null || userInfo == null) {
            return ResponseUtil.badArgument();
        }

        String sessionKey = null;
        String openId = null;
        if (isLoginByH5) {
            sessionKey = wxLoginInfo.getUserInfo().getAccessToken();
            openId = wxLoginInfo.getUserInfo().getWxOpenId();
        } else {
            try {
                WxMaJscode2SessionResult result = this.wxService.getUserService().getSessionInfo(code);
                sessionKey = result.getSessionKey();
                openId = result.getOpenid();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (sessionKey == null || openId == null) {
            return ResponseUtil.fail();
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPassword = encoder.encode(openId);

        LitemallUser user = userService.queryByOid(openId);
        if (user == null) {
            user = new LitemallUser();
            user.setUsername(openId);
            user.setPassword(encodedPassword);
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
            try {
                memberService.checkMemberStatus(user);
            } catch (IllegalArgumentException | MemberOrderDataException | DataStatusException e) {
                return ResponseUtil.fail(AUTH_USERS_MEMBER_STATUS, e.getMessage());
            } // 检查会员订单状态

            user.setLastLoginTime(LocalDateTime.now());
            user.setLastLoginIp(IpUtil.getIpAddr(request));
            user.setSessionKey(sessionKey);
            if (userService.updateById(user) == 0) {
                return ResponseUtil.updatedDataFailed();
            }
        }

        // token
        String token = UserTokenManager.generateToken(user.getId());
        userInfo = initUserInfo(user, user.getUsername());

        Map<Object, Object> result = new HashMap<Object, Object>();
        result.put("weAccessToken", wxLoginInfo.getUserInfo().getAccessToken());
        result.put("token", token);
        result.put("userInfo", userInfo);
        return ResponseUtil.ok(result);
    }

    @PostMapping("manual_bind_phone")
    public Object manualBindPhone(@RequestBody String body, HttpServletRequest request) throws IOException {
        String token = JacksonUtil.parseString(body, "token");
        String phone = JacksonUtil.parseString(body, "phone");
        String code = JacksonUtil.parseString(body, "code");
        if (StringUtils.isEmpty(token) || StringUtils.isEmpty(phone)) {
            return ResponseUtil.badArgument();
        }
        if (!RegexUtil.isMobileSimple(phone)) {
            return ResponseUtil.badArgumentValue();
        }

        //判断验证码是否正确
        String cacheCode = CaptchaCodeManager.getCachedCaptcha(phone);
        if (cacheCode == null || cacheCode.isEmpty() || !cacheCode.equals(code)) {
            return ResponseUtil.fail(AUTH_CAPTCHA_UNMATCH, "验证码错误");
        }

        return loginThenCheckAndUpdatePhone(token, phone);
    }
    

    @PostMapping("bind_trader")
    public Object bindTrader(@RequestBody String body, HttpServletRequest request) throws IOException {
        String token = JacksonUtil.parseString(body, "token");
        String traderName = JacksonUtil.parseString(body, "name");
        String taxid = JacksonUtil.parseString(body, "taxid");
        if (StringUtils.isEmpty(token) || StringUtils.isEmpty(traderName) || StringUtils.isEmpty(taxid )) {
            return ResponseUtil.badArgument();
        }
        if (taxid.length() != 18) {
            return ResponseUtil.badArgumentValue();
        }

        Integer userId = UserTokenManager.getUserId(token);
        if (userId == null) {
            return ResponseEntity.status(501).build(); // 未登录
        }

        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(404).build(); // 用户不存在
        }

        LitemallTrader trader = new LitemallTrader();
        trader.setCompanyName(traderName);
        trader.setTaxid(taxid);
        LitemallTrader bindedTrader = traderService.registerUser(user, trader);

        return ResponseUtil.ok(bindedTrader);
    }

    private Object loginThenCheckAndUpdatePhone(String token, String phone) {
        Integer userId = UserTokenManager.getUserId(token);
        if (userId == null) {
            return ResponseEntity.status(501).build(); // 未登录
        }

        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(404).build(); // 用户不存在
        }

        if (user.getMobile() != null && user.getMobile().length() > 0) {
            return ResponseUtil.fail(AUTH_USERS_PHONE_BIND, "已绑定手机号 " + user.getMobile());
        }

        Object validate = validatePhone(user, phone);
        if (validate != null) {
            return validate;
        }

        user.setMobile(phone);
        if(user.getWeixinOpenid() != null && user.getWeixinOpenid().length() > 0) {
            user.setUsername(phone); // 微信用户需要将手机号作为用户名
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            //获取手机后6位作为密码
            String encodedPassword = encoder.encode(phone.substring(phone.length() - 6));
            user.setPassword(encodedPassword);
        }
        if (userService.updateById(user) == 0) {
            return ResponseUtil.updatedDataFailed();
        }        
        return ResponseUtil.ok();
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
        Integer codeType = JacksonUtil.parseInteger(body, "type");
        if (StringUtils.isEmpty(phoneNumber)) {
            return ResponseUtil.badArgument();
        }
        if (!RegexUtil.isMobileSimple(phoneNumber)) {
            return ResponseUtil.badArgumentValue();
        }

        if (codeType != null) {
            if (codeType.equals(CharUtil.CharType.FORGET_PASSWORD.getTypeValue())) {
                List<LitemallUser> userList = userService.queryByUsername(phoneNumber);
                if (userList.size() > 1) {
                    return ResponseUtil.serious();
                } else if (userList.size() == 0) {
                    return ResponseUtil.fail(AUTH_MOBILE_UNREGISTERED, "未注册的手机号");
                }
            }
        }

        if (!notifyService.isSmsEnable()) {
            return ResponseUtil.fail(AUTH_CAPTCHA_UNSUPPORT, "后台验证码服务未开启");
        }
        String code = CharUtil.getRandomNum(6);
        boolean successful = CaptchaCodeManager.addToCache(phoneNumber, code);
        if (!successful) {
            return ResponseUtil.fail(AUTH_CAPTCHA_FREQUENCY, "验证码未超时1分钟，不能发送");
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", code);
        notifyService.notifySmsTemplate(phoneNumber, NotifyType.CAPTCHA, params);

        return ResponseUtil.ok();
    }

    @PostMapping("can_register_phone")
    public Object isCanRegisterPhone(@RequestBody String body, HttpServletRequest request) {
        String phone = JacksonUtil.parseString(body, "phone");
        if (StringUtils.isEmpty(phone)) {
            return ResponseUtil.badArgument();
        }
        if (!RegexUtil.isMobileSimple(phone)) {
            return ResponseUtil.fail(AUTH_INVALID_MOBILE, "手机号格式不正确");
        }

        Object result = validatePhone(null, phone);
        if (result != null) {
            return result;
        }

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

        Object validate = validatePhone(null, mobile);
        if (validate != null) {
            return validate;
        }

        if (!RegexUtil.isMobileSimple(mobile)) {
            return ResponseUtil.fail(AUTH_INVALID_MOBILE, "手机号格式不正确");
        }
        //判断验证码是否正确
        String cacheCode = CaptchaCodeManager.getCachedCaptcha(mobile);
        if (cacheCode == null || cacheCode.isEmpty() || !cacheCode.equals(code)) {
            return ResponseUtil.fail(AUTH_CAPTCHA_UNMATCH, "验证码错误");
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
        UserInfo userInfo = initUserInfo(user, username);
        userInfo.setNickName(username);

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
            return ResponseUtil.fail(AUTH_CAPTCHA_UNSUPPORT, "后台验证码服务未开启");
        }
        String code = CharUtil.getRandomNum(6);
        boolean successful = CaptchaCodeManager.addToCache(phoneNumber, code);
        if (!successful) {
            return ResponseUtil.fail(AUTH_CAPTCHA_FREQUENCY, "验证码未超时1分钟，不能发送");
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", code);
        notifyService.notifySmsTemplate(phoneNumber, NotifyType.CAPTCHA, params);

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
    public Object resetPassword(@LoginUser Integer userId, @RequestBody String body, HttpServletRequest request) {
        LitemallUser user = null;
        boolean isChangePassword = JacksonUtil.hasField(body, "oldPassword") && JacksonUtil.hasField(body, "newPassword");
        boolean isValidateCodeReset = JacksonUtil.hasField(body, "password");

        //登录后修改密码的方式
        String oldPassword = JacksonUtil.parseString(body, "oldPassword");
        String newPassword = JacksonUtil.parseString(body, "newPassword");
        // 验证码重置的方式
        String password = JacksonUtil.parseString(body, "password");

        String mobile = JacksonUtil.parseString(body, "mobile");
        String code = JacksonUtil.parseString(body, "code");

        if (mobile == null || code == null || (isChangePassword && (newPassword == null || oldPassword == null)) || (isValidateCodeReset && password == null)) {
            return ResponseUtil.badArgument();
        }
        
        if (isValidateCodeReset) {
            newPassword = password;
            oldPassword = null;

            //判断验证码是否正确
            String cacheCode = CaptchaCodeManager.getCachedCaptcha(mobile);
            if (cacheCode == null || cacheCode.isEmpty() || !cacheCode.equals(code))
                return ResponseUtil.fail(AUTH_CAPTCHA_UNMATCH, "验证码错误");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (isChangePassword) {
            if(userId == null){
                return ResponseUtil.unlogin();
            }

            user = userService.findById(userId);
            if (user == null) {
                return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "用户不存在");
            }

            if (!encoder.matches(oldPassword, user.getPassword())) {
                return ResponseUtil.fail(AUTH_INVALID_PASSWORD, "原登录密码不正确");
            }
        }

        List<LitemallUser> userList = userService.queryByMobile(mobile);
        if (userList.size() > 1) {
            return ResponseUtil.serious();
        } else if (userList.size() == 0) {
            return ResponseUtil.fail(AUTH_MOBILE_UNREGISTERED, "手机号未注册");
        } else {
            user = userList.get(0);
        }
        if (isChangePassword && !user.getId().equals(userId)) {
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

        if (!RegexUtil.isMobileSimple(mobile)) {
            return ResponseUtil.fail(AUTH_INVALID_MOBILE, "手机号格式不正确");
        }

        //判断验证码是否正确
        String cacheCode = CaptchaCodeManager.getCachedCaptcha(mobile);
        if (cacheCode == null || cacheCode.isEmpty() || !cacheCode.equals(code))
            return ResponseUtil.fail(AUTH_CAPTCHA_UNMATCH, "验证码错误");


        LitemallUser user = userService.findById(userId);

        Object validate =  validatePhone(user, mobile);
        if (validate != null) {
            return validate;
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(password, user.getPassword())) {
            return ResponseUtil.fail(AUTH_INVALID_PASSWORD, "账号密码不正确");
        }

        //更新头像
        String avatar = user.getAvatar();
        String localAvatarPath = storageService.getLocalStorage().getStoragePath() + avatar;
        //检查头像文件是否存在
        File avatarFile = new File(localAvatarPath);
        if (avatarFile.exists()) {
            //如果存在，则将头像文件名也改为手机号
            String fileExtension = avatar.substring(avatar.lastIndexOf("."));
            String newAvatarFileName = mobile + fileExtension;
            String newAvatarPath = storageService.getLocalStorage().getStoragePath() + getAvatarFolder() + newAvatarFileName;
            File newAvatarFile = new File(newAvatarPath);
            if (avatarFile.renameTo(newAvatarFile)) {
                //头像文件名修改成功
                user.setAvatar(getAvatarFolder() + newAvatarFileName);
            } else {
                //头像文件名修改失败，返回错误
                return ResponseUtil.fail(502, "头像文件名修改失败");
            }
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
        String avatarUrl =  getAvatarFolder() +  avatarFileName;
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

        Object validationResult = validatePhone(user, phone);
        if (validationResult != null) {
            return validationResult;
        }

        user.setMobile(phone);
        user.setUsername(phone); // 手机号作为用户名
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
        return storageService.getLocalStorage().getAvatarFolder();
    }    

    private String getAvatarAbsFolder() {
        return storageService.getLocalStorage().getAvatarAbsFolder();
    }

    private Object validatePhone(LitemallUser user, String phone) {
        if (user == null || user.getUsername().equals(phone) == false) {
            List<LitemallUser> userList = userService.queryByUsername(phone);
            if (userList.size() > 0) {
                return ResponseUtil.fail(AUTH_NAME_REGISTERED, "该手机号已被注册");
            }
        }

        if (user == null || user.getMobile().equals(phone) == false) {
            List<LitemallUser> userList = userService.queryByMobile(phone);
            if (userList.size() > 0) {
                return ResponseUtil.fail(AUTH_NAME_REGISTERED, "该手机号已被绑定");
            }
        }
        return null;
    }
}
