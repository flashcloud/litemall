<template>
	<div class="login">
    	<div class="store_header">
		<div class="store_avatar">
			<img src="../../assets/images/avatar_default.png" alt="头像" width="55" height="55">
		</div>
		<div class="store_name">金软在线订购平台</div>
	</div>

    <md-field-group>
      <md-field
        v-model="account"
        icon="username"
        placeholder="用户名"
        right-icon="clear-full"
        name="user"
        data-vv-as="帐号"
        @right-click="clearText"
      />

      <md-field
        v-model="password"
        icon="lock"
        placeholder="登录密码"
        :type="visiblePass ? 'text' : 'password'"
        :right-icon="visiblePass ? 'eye-open' : 'eye-close'"
        data-vv-as="密码"
        name="password"
        @right-click="visiblePass = !visiblePass"
      />

      <div class="clearfix">
        <div class="float-l">
          <router-link to="/login/registerGetCode">免费注册</router-link>
        </div>
        <div class="float-r">
          <router-link to="/login/forget">忘记密码</router-link>
        </div>
      </div>

      <van-button size="large" type="danger" :loading="isLogining" @click="loginSubmit">登录</van-button>
    </md-field-group>


      <div class="text-desc text-center bottom_positon">Copyright 2008 - 2025 成都市上谷科技有限公司. All Rights Reserved <br> <a href="https://beian.miit.gov.cn" target="_blank" title="工信部域名备案查询">蜀ICP备17034488号</a></div>

	</div>
</template>

<script>
import field from '@/components/field/';
import fieldGroup from '@/components/field-group/';

import { authLoginByAccount, getWeChatCode, authLoginByWeixin } from '@/api/api';
import { setLocalStorage } from '@/utils/local-storage';
import { emailReg, mobileReg } from '@/utils/validate';

import { Toast } from 'vant';

function isWeChatBrowser() {
    var ua = navigator.userAgent.toLowerCase();
    return ua.indexOf('micromessenger') !== -1;
}

// 登录成功后的处理逻辑
function after_login(self, loginFunc, loginData) {
    loginFunc(loginData).then(res => {
        self.userInfo = res.data.data.userInfo;
        setLocalStorage({
          Authorization: res.data.data.token,
          avatar: self.userInfo.avatarUrl,
          nickName: self.userInfo.nickName,
          memberType: self.userInfo.memberType,
          memberPlan: self.userInfo.memberPlan,
          memberExpire: self.userInfo.memberExpire
        });

        if (!self.userInfo.mobile || self.userInfo.mobile == '') {
          self.$router.push({ name: 'bindPhone' });
        } else {
          self.routerRedirect();
        }
      }).catch(error => {
        if (error.data) {
            Toast.fail(error.data.errmsg);
        } else {
            Toast.fail('登录失败，请重试' + error.toString());
        }
      });  
}

export default {
  name: 'login-request',
  components: {
    [field.name]: field,
    [fieldGroup.name]: fieldGroup,
    Toast
  },

  created() {
    //用微信扫本登录页面网址二维码，在微信内浏览器用微信登录
    if (isWeChatBrowser()) {
      const href = window.location.href;
      if (href.indexOf('?code=') > -1 && href.endsWith('#/login')) {
        //微信回调的网址有误，修复
        let newUrl = href.replace('#/login', '');
        newUrl = newUrl.replace('?code=', '#/login?code=');
        window.location.href = newUrl;
        return;
      }
      const wechatCode = this.$route.query.code;
      const state = this.$route.query.state;
      if (wechatCode !== undefined) {
        // 微信浏览器内，用code获取openid来使用微信登录
        after_login(this, authLoginByWeixin, { code: wechatCode, state: state });
        return;
      }

      // 微信浏览器，获取code
      getWeChatCode({ router: 'login'}).then(res => {
          const weLoginUrl = res.data.data;
          window.location.href = weLoginUrl;
      }).catch(error => {
          Toast.fail(error.data.errmsg);
      });
    }
  },

  data() {
    return {
      account: '',
      password: '',
      visiblePass: false,
      isLogining: false,
      userInfo: {}
    };
  },

  methods: {
    clearText() {
      this.account = '';
    },

    validate() {

    },

    login() {
      let loginData = this.getLoginData();
      after_login(this, authLoginByAccount, loginData);
    },

    loginSubmit() {
      this.isLogining = true;
      try {
        this.validate();
        this.login();
        this.isLogining = false;
      } catch (err) {
        console.log(err.message);
        this.isLogining = false;
      }
    },

    routerRedirect() {
      // const { query } = this.$route;
      // this.$router.replace({
      //   name: query.redirect || 'home',
      //   query: query
      // });
      window.location = '#/user/';
    },

    getLoginData() {
      const password = this.password;
      const account = this.getUserType(this.account);
      return {
        [account]: this.account,
        password: password
      };
    },

    getUserType(account) {
      const accountType = mobileReg.test(account)
        ? 'mobile'
        : emailReg.test(account)
        ? 'email'
        : 'username';
      return accountType;
    }
  }
};
</script>


<style lang="scss" scoped>
@import '../../assets/scss/mixin';
.login {
  position: relative;
  background-color: #fff;
}
.store_header {
  text-align: center;
  padding: 30px 0;
  .store_avatar img {
    border-radius: 50%;
  }
  .store_name {
    padding-top: 5px;
    font-size: 16px;
  }
}
.register {
  padding-top: 40px;
  color: $font-color-gray;
  a {
    color: $font-color-gray;
  }
  > div {
    width: 50%;
    box-sizing: border-box;
    padding: 0 20px;
  }
  .connect {
    @include one-border(right);
    text-align: right;
  }
}
.bottom_positon {
  position: absolute;
  bottom: 30px;
  width: 100%;
}
</style>
