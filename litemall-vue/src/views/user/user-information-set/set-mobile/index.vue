<template>
	<div>
		<van-cell-group>
			<van-field
				label="原登录密码"
				v-model="password"
				type="password"
				placeholder="请输入旧的登录密码"
				 />

			<van-field
				label="新手机号"
				v-model="mobile"
				placeholder="请输入新手机号"
				/>

			<!-- <van-field
				label="验证码"
				v-model="code"
				@click-icon="getCode"
				placeholder="请输入验证码">

				<span slot="icon"
					class="verifi_code red"
					:class="{verifi_code_counting: counting}"
					@click="getCode">
					<countdown v-if="counting" :time="60000" @end="countdownend">
					  <template slot-scope="props">{{ +props.seconds || 60 }}秒后获取</template>
					</countdown>
					<span v-else>获取验证码</span>
				</span>
			</van-field> -->

			<van-field
				label="验证码"
				v-model="code"
				@click-icon="getCode"
				placeholder="请输入验证码">

				<span slot="button"
					class="verifi_code red"
					:class="{verifi_code_counting: counting}"
					@click="getCode">
					<countdown v-if="counting" :time="60000" @end="countdownend">
					  <template slot-scope="props">{{ +props.seconds || 60 }}秒后获取</template>
					</countdown>
					<span v-else>获取验证码</span>
				</span>
			</van-field>            
		</van-cell-group>

		<div class="bottom_btn">
			<van-button size="large" type="danger" @click="saveMobile">保存</van-button>
		</div>
	</div>
</template>


<script>
import { authCaptcha, authResetPhone, authLogout } from '@/api/api';

import { Field } from 'vant';
import { removeLocalStorage } from '@/utils/local-storage';

export default {
  data: () => ({
    password: '',
    mobile: '',
    code: '',
    counting: false
  }),

  methods: {
    getCode() {
      if (!this.counting && this.vuelidate()) {
        authCaptcha({
          mobile: this.mobile,
          type: 'bind-mobile'
        }).then(() => {
          this.$toast.success('发送成功');
          this.counting = true;
        }).catch(error => {
          this.$toast.fail(error.data.errmsg);
          this.counting = false;
        })

      }
    },
    countdownend() {
      this.counting = false;
    },
    vuelidate() {
      if(this.password === ''){
        this.$toast.fail('请输入原登录密码');
        return false;
      }        
      if(this.mobile === ''){
        this.$toast.fail('请输入新手机号');
        return false;
      }
      return true;
    },
    loginOut() {
      authLogout().then(res => {
        removeLocalStorage('Authorization')
        removeLocalStorage('avatar')
        removeLocalStorage('nickName')
        removeLocalStorage('memberType')
        removeLocalStorage('memberPlan')
        removeLocalStorage('memberExpire')        
        this.$router.push({ name: 'login' });
      });

    },    
    saveMobile() {
        if(this.code === ''){
            this.$toast.fail('请输入验证码');
            return false;
        }
        if (this.vuelidate()) {
            authResetPhone({
                password: this.password,
                mobile: this.mobile,
                code: this.code
            })
            .then(() => {
                this.$dialog.alert({ message: '保存成功, 请使用新手机号' + this.mobile + '重新登录.' })
                this.loginOut();
            }).catch (error => {
                Toast.fail(error.data.errmsg);
            });
        }
    }
  },

  components: {
    [Field.name]: Field
  }
};
</script>

<style lang="scss" scoped>
@import '../../../../assets/scss/var';
@import '../../../../assets/scss/mixin';
.bottom_btn {
  padding: 30px 15px 0 15px;
}

.verifi_code {
  @include one-border;
  padding-left: 10px;
  &::after {
    border-bottom: 0;
    border-left: 1px solid $border-color;
  }

  &_counting {
    color: $font-color-gray;
  }
}
</style>
