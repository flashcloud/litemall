<template>
  <div class="user_header" :style="{backgroundImage: `url(${background_image})`}">
    <van-icon name="set" class="user_set" @click="toSetting"/>
    <div class="user_avatar">
      <img :src="avatar" alt="头像" width="60" height="60">
    </div>
    <div>{{nickName}} <van-tag type="success">{{memberDes}}</van-tag></div>
  </div>
</template>

<script>
import { Tag } from 'vant';
import avatar_default from '@/assets/images/avatar_default.png';
import bg_default from '@/assets/images/user_head_bg.png';
import { getLocalStorage } from '@/utils/local-storage';

export default {
  name: 'user-header',

  props: {
    isLogin: {
      type: Boolean,
      default: false
    }
  },

  data() {
    return {
      nickName: '昵称',
      avatar: avatar_default,
      token: '',
      memberDes: '',
      background_image: bg_default
    };
  },

  activated() {
    this.getUserInfo();
  },

  methods: {
    getUserInfo() {
      const infoData = getLocalStorage(
        'nickName',
        'avatar',
        'Authorization',
        'memberDes'
      );
      
      this.avatar = infoData.avatar || avatar_default;
      this.nickName = infoData.nickName || '昵称';
      this.token = infoData.Authorization || '';
      this.memberDes = infoData.memberDes || '普通会员';

      if(this.avatar.indexOf('/images/avatar/') > -1 && this.token !== '') {
        this.avatar = '/wx/auth/getAvatar?token=' + this.token;
      }      
    },
    toSetting() {
      this.$router.push({ name: 'user-information' });
    }
  },

  components: {
    [Tag.name]: Tag,
  }  
};
</script>

<style lang="scss" scoped>
.user_header {
  background-repeat: no-repeat;
  background-size: cover;
  height: 130px;
  text-align: center;
  color: #fff;
  padding-top: 30px;
}

.user_set {
  position: absolute;
  top: 10px;
  right: 10px;
  font-size: 24px;
}
.user_avatar {
  margin-bottom: 10px;
  img {
    border: 0;
    border-radius: 50%;
  }
}
</style>
