<template>
  <div>
    <van-cell-group style="background-color: transparent;">
      <van-cell class="member-card" @click="openMember">
        <div class="member-content">
          <div class="member-title">开通金软助手普通会员</div>
          <div class="member-price">最低 0.3 元/天</div>
        </div>
      </van-cell>
    </van-cell-group>
  </div>
</template>

<script>
export default {
  name: 'open-member', 

  methods: {
    openMember() {
      // 跳转到会员开通页面或显示会员套餐选择
      this.$router.push({ name: 'user-member-scheme-list' });
    }
  }
};
</script>

<style scoped lang="scss">
@import '../../assets/scss/mixin';

.member-card {
  background: linear-gradient(135deg, #f5e6d3 0%, #e9d5c1 100%);
  border-radius: 12px;
  margin: 10px 10px 10px 10px;
  padding: 10px;
  width: 95%;
  
  .van-cell {
    background: transparent;
    border-radius: 12px;
    padding: 24px 16px;
  }
}

.member-content {
    
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  
  .member-title {
    font-size: 14px;
    font-weight: bold;
    color: #333;
  }
  
  .member-price {
    font-size: 14px;
    color: #666;
    background-color: rgba(255, 255, 255, 0.8);
    padding: 6px 12px;
    border-radius: 16px;
  }
}
</style>
